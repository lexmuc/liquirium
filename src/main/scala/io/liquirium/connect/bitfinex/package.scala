package io.liquirium.connect

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.connect.bitfinex.BitfinexRestApi.BitfinexApiRequest
import io.liquirium.core._
import io.liquirium.util.akka._
import io.liquirium.util.{ApiCredentials, MillisecondNonceGenerator, SystemClock}

import java.time.{Duration, Instant}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

package object bitfinex {

  val exchangeId: ExchangeId = ExchangeId("BITFINEX")

  type AsyncBitfinexApi = AsyncApi[BitfinexApiRequest[_]]

  private val jsonConverter = new BitfinexJsonConverter()

  private val modelConverter = new BitfinexModelConverter(exchangeId)

  private val apiLogger = io.liquirium.util.Logger("bitfinex-api")
  private val nonceGenerator = new MillisecondNonceGenerator(SystemClock)

  private def authenticator(credentials: ApiCredentials) =
    BitfinexAuthenticator(credentials.apiKey, credentials.secret)

  def bitfinexHttpService(
    concurrencyContext: ConcurrencyContext,
    authenticator: BitfinexAuthenticator,
  ): BitfinexHttpService =
    new BitfinexHttpService(
      concurrencyContext.asyncHttpService,
      authenticator,
      nonceGenerator,
      BitfinexResponseTransformer,
      apiLogger
    )(
      concurrencyContext.executionContext
    )

  def asyncWrappedRestApi(
    concurrencyContext: ConcurrencyContext,
    credentials: ApiCredentials,
  ): Future[AsyncBitfinexApi] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    val spawner = concurrencyContext.spawner
    val extendedHttpService = bitfinexHttpService(concurrencyContext, authenticator(credentials))
    val coreApi = new BitfinexRestApi(extendedHttpService, jsonConverter, apiLogger)
    for {
      coreActor <- spawner.spawnAsync(
        AsyncApiAdapter.actorBasedApi(coreApi),
        "bitfinex-core-api",
      )
      throttler <- spawner.spawnAsync(
        AsyncRequestThrottler.behavior(coreActor, 200.millis),
        "bitfinex-request-throttler",
      )
      sequencer <- spawner.spawnAsync(
        AsyncRequestSequencer.forActor(throttler),
        "bitfinex-request-sequencer",
      )
    } yield AsyncApiAdapter.futureBasedApi(sequencer)(concurrencyContext.actorSystem)
  }

  def api(concurrencyContext: ConcurrencyContext, credentials: ApiCredentials): Future[GenericExchangeApi] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    for {
      restApi <- asyncWrappedRestApi(concurrencyContext, credentials)
    } yield new BitfinexApiAdapter(
      restApi,
      modelConverter,
      maxCandleBatchSize = 5000,
      maxTradeBatchSize = 1000,
      maxOrderHistoryBatchSize = 1000,
    )
  }

  def getConnector(
    concurrencyContext: ConcurrencyContext = DefaultConcurrencyContext,
    credentials: ApiCredentials = ApiCredentials("", ""),
  ): Future[ExchangeConnector] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    api(concurrencyContext, credentials).map {
      bitfinexApi => makeConnector(bitfinexApi, concurrencyContext)
    }
  }

  private def makeConnector(
    bitfinexApi: GenericExchangeApi,
    concurrencyContext: ConcurrencyContext,
  )(implicit ec: ExecutionContext) =
    new ExchangeConnector {

      private def makeCandleHistorySegmentLoader(
        tradingPair: TradingPair,
        candleLength: Duration,
      ): LiveCandleHistoryLoader =
        new LiveCandleHistoryLoader(
          start => bitfinexApi.getCandleBatch(tradingPair, candleLength, start),
          candleLength = candleLength,
        )

      private def makeCandleHistoryStream(tradingPair: TradingPair, candleLength: Duration) = {
        new PollingCandleHistoryStream(
          segmentLoader =
            start => makeCandleHistorySegmentLoader(tradingPair, candleLength).load(start, SystemClock.getTime),
          interval = FiniteDuration(candleLength.getSeconds / 2, "seconds"),
          retryInterval = FiniteDuration(10, "seconds"),
          updateOverlapStrategy = chs => chs.end.minusMillis(2 * candleLength.toMillis),
          sourceQueueFactory = concurrencyContext.sourceQueueFactory,
        )
      }

      override def loadCandleHistory(
        tradingPair: TradingPair,
        duration: Duration,
        start: Instant,
        end: Instant,
      ): Future[CandleHistorySegment] =
        makeCandleHistorySegmentLoader(tradingPair, duration).load(start, end)

      override def candleHistoryStream(
        tradingPair: TradingPair,
        initialSegment: CandleHistorySegment,
      ): Source[CandleHistorySegment, NotUsed] =
        makeCandleHistoryStream(tradingPair, initialSegment.candleLength).source(initialSegment)

      private def makeTradeHistorySegmentLoader(pair: TradingPair) =
        new TradeHistorySegmentLoader(start => bitfinexApi.getTradeBatch(pair, start))

      private def makeTradeHistoryStream(tradingPair: TradingPair) =
        new PollingTradeHistoryStream(
          segmentLoader = makeTradeHistorySegmentLoader(tradingPair).loadFrom,
          interval = FiniteDuration(30, "seconds"),
          retryInterval = FiniteDuration(10, "seconds"),
          updateOverlapStrategy = TradeUpdateOverlapStrategy.fixedOverlap(Duration.ofMinutes(5)),
          sourceQueueFactory = concurrencyContext.sourceQueueFactory,
        )

      override def loadTradeHistory(tradingPair: TradingPair, start: Instant): Future[TradeHistorySegment] =
        makeTradeHistorySegmentLoader(tradingPair).loadFrom(start)

      def tradeHistoryStream(
        tradingPair: TradingPair,
        initialSegment: TradeHistorySegment,
      ): Source[TradeHistorySegment, NotUsed] =
        makeTradeHistoryStream(tradingPair).source(initialSegment)

      override def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]] =
        bitfinexApi.sendTradeRequest(request)

      override def openOrdersStream(tradingPair: TradingPair): Source[Set[Order], NotUsed] = ???

    }

}
