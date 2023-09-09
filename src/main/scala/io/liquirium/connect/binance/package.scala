package io.liquirium.connect

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.connect.binance.BinanceRestApi.BinanceApiRequest
import io.liquirium.core.{CandleHistorySegment, ExchangeId, Market, OperationRequest, OperationRequestSuccessResponse, Order, TradeHistorySegment, TradingPair}
import io.liquirium.util.akka._
import io.liquirium.util.{ApiCredentials, SystemClock}

import java.time.{Duration, Instant}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

package object binance {

  val exchangeId: ExchangeId = ExchangeId("BINANCE")

  type AsyncBinanceApi = AsyncApi[BinanceApiRequest[_]]

  private val jsonConverter = new BinanceJsonConverter()

  private val modelConverter = new BinanceModelConverter(exchangeId)

  private val apiLogger = io.liquirium.util.Logger("binance-api")

  private def authenticator(credentials: ApiCredentials) =
    BinanceAuthenticator(credentials.apiKey, credentials.secret)

  def asyncExtendedHttpService(
    concurrencyContext: ConcurrencyContext,
    authenticator: BinanceAuthenticator,
  ): BinanceExtendedHttpService = {
    val baseService = new BinanceHttpService(concurrencyContext.asyncHttpService, authenticator, SystemClock)
    new BinanceExtendedHttpService(baseService)(concurrencyContext.executionContext)
  }

  def asyncWrappedRestApi(
    concurrencyContext: ConcurrencyContext,
    credentials: ApiCredentials,
  ): Future[AsyncBinanceApi] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    val spawner = concurrencyContext.spawner
    val extendedHttpService = asyncExtendedHttpService(concurrencyContext, authenticator(credentials))
    val coreApi = new BinanceRestApi(extendedHttpService, jsonConverter, apiLogger)
    for {
      coreActor <- spawner.spawnAsync(
        AsyncApiAdapter.actorBasedApi(coreApi),
        "binance-core-api",
      )
      throttler <- spawner.spawnAsync(
        AsyncRequestThrottler.behavior(coreActor, 200.millis),
        "binance-request-throttler",
      )
      sequencer <- spawner.spawnAsync(
        AsyncRequestSequencer.forActor(throttler),
        "binance-request-sequencer",
      )
    } yield AsyncApiAdapter.futureBasedApi(sequencer)(concurrencyContext.actorSystem)
  }

  def api(concurrencyContext: ConcurrencyContext, credentials: ApiCredentials): Future[GenericExchangeApi] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    for {
      restApi <- asyncWrappedRestApi(concurrencyContext, credentials)
    } yield new BinanceApiAdapter(restApi, modelConverter)
  }

  def getConnector(
    concurrencyContext: ConcurrencyContext = DefaultConcurrencyContext,
    credentials: ApiCredentials = ApiCredentials("", ""),
  ): Future[ExchangeConnector] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    api(concurrencyContext, credentials).map {
      binanceApi => makeConnector(binanceApi, concurrencyContext)
    }
  }

  private def makeConnector(
    binanceApi: GenericExchangeApi,
    concurrencyContext: ConcurrencyContext,
  )(implicit ec: ExecutionContext) =
    new ExchangeConnector {

      // candles

      private def makeCandleHistorySegmentLoader(
        tradingPair: TradingPair,
        candleLength: Duration,
      ): LiveCandleHistoryLoader =
        new LiveCandleHistoryLoader(
          start => binanceApi.getCandleBatch(tradingPair, candleLength, start),
          candleLength = candleLength,
        )

      override def loadCandleHistory(
        tradingPair: TradingPair,
        duration: Duration,
        start: Instant,
      ): Future[CandleHistorySegment] =
        makeCandleHistorySegmentLoader(tradingPair, duration).load(start, SystemClock.getTime)

      private def makeCandleHistoryStream(tradingPair: TradingPair, candleLength: Duration) =
        new PollingCandleHistoryStream(
          segmentLoader =
            start => makeCandleHistorySegmentLoader(tradingPair, candleLength).load(start, SystemClock.getTime),
          interval = FiniteDuration(candleLength.getSeconds / 2, "seconds"),
          retryInterval = FiniteDuration(10, "seconds"),
          updateOverlapStrategy = CandleUpdateOverlapStrategy.numberOfCandles(2),
          sourceQueueFactory = concurrencyContext.sourceQueueFactory,
        )

      override def candleHistoryStream(
        tradingPair: TradingPair,
        initialSegment: CandleHistorySegment,
      ): Source[CandleHistorySegment, NotUsed] =
        makeCandleHistoryStream(tradingPair, initialSegment.candleLength).source(initialSegment)

      // trades

      private def makeTradeHistorySegmentLoader(tradingPair: TradingPair): TradeHistorySegmentLoader =
        new TradeHistorySegmentLoader(start => binanceApi.getTradeBatch(tradingPair, start))

      override def loadTradeHistory(tradingPair: TradingPair, start: Instant): Future[TradeHistorySegment] =
        makeTradeHistorySegmentLoader(tradingPair).loadFrom(start)

      private def makeTradeHistoryStream(tradingPair: TradingPair) =
        new PollingTradeHistoryStream(
          segmentLoader = makeTradeHistorySegmentLoader(tradingPair).loadFrom,
          interval = FiniteDuration(30, "seconds"),
          retryInterval = FiniteDuration(10, "seconds"),
          updateOverlapStrategy = TradeUpdateOverlapStrategy.fixedOverlap(Duration.ofMinutes(5)),
          sourceQueueFactory = concurrencyContext.sourceQueueFactory,
        )

      def tradeHistoryStream(
        tradingPair: TradingPair,
        initialSegment: TradeHistorySegment,
      ): Source[TradeHistorySegment, NotUsed] = makeTradeHistoryStream(tradingPair).source(initialSegment)

      override def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]] =
        binanceApi.sendTradeRequest(request)

      override def openOrdersStream(tradingPair: TradingPair): Source[Set[Order], NotUsed] = {
        OpenOrdersStream.fromOrdersProvider(
          optMarket = Some(Market(exchangeId, tradingPair)),
          interval = 40.seconds,
          retryDelay = 10.seconds,
          openOrdersProvider = optMarket => binanceApi.getOpenOrders(optMarket.get.tradingPair),
          apiLogger
        ).map(_.openOrders)
      }

    }

}
