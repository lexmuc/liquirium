package io.liquirium.connect

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.core.{CandleHistorySegment, ExchangeId, OperationRequest, OperationRequestSuccessResponse, Order, TradeHistorySegment, TradingPair}
import io.liquirium.util.akka._
import io.liquirium.util.{ApiCredentials, SystemClock}

import java.time.Duration
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}


package object coinbase {

  val exchangeId: ExchangeId = ExchangeId("COINBASE")

  type AsyncCoinbaseApi = AsyncApi[CoinbaseApiRequest[_]]

  private val jsonConverter = new CoinbaseJsonConverter()

  private val modelConverter = new CoinbaseModelConverter(exchangeId)

  private val apiLogger = io.liquirium.util.Logger("coinbase-api")

  private def authenticator(credentials: ApiCredentials) =
    new CoinbaseAuthenticator(credentials, SystemClock)

  def coinbaseHttpService(
    concurrencyContext: ConcurrencyContext,
    authenticator: CoinbaseAuthenticator,
  ): CoinbaseHttpService =
    new CoinbaseHttpService(
      concurrencyContext.asyncHttpService,
      authenticator,
      CoinbaseResponseTransformer,
      apiLogger
    )(
      concurrencyContext.executionContext
    )

  def asyncWrappedRestApi(
    concurrencyContext: ConcurrencyContext,
    credentials: ApiCredentials,
  ): Future[AsyncCoinbaseApi] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    val spawner = concurrencyContext.spawner
    val extendedHttpService = coinbaseHttpService(concurrencyContext, authenticator(credentials))
    val coreApi = new CoinbaseRestApi(extendedHttpService, jsonConverter, apiLogger)
    for {
      coreActor <- spawner.spawnAsync(
        AsyncApiAdapter.actorBasedApi(coreApi),
        "coinbase-core-api",
      )
      throttler <- spawner.spawnAsync(
        AsyncRequestThrottler.behavior(coreActor, 200.millis),
        "coinbase-request-throttler",
      )
      sequencer <- spawner.spawnAsync(
        AsyncRequestSequencer.forActor(throttler),
        "coinbase-request-sequencer",
      )
    } yield AsyncApiAdapter.futureBasedApi(sequencer)(concurrencyContext.actorSystem)
  }

  def api(concurrencyContext: ConcurrencyContext, credentials: ApiCredentials): Future[GenericExchangeApi] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    for {
      restApi <- asyncWrappedRestApi(concurrencyContext, credentials)
    } yield new CoinbaseApiAdapter(restApi, modelConverter, clock = SystemClock)
  }

  def getConnector(
    concurrencyContext: ConcurrencyContext = DefaultConcurrencyContext,
    credentials: ApiCredentials = ApiCredentials("", ""),
  ): Future[ExchangeConnector] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    api(concurrencyContext, credentials).map {
      coinbaseApi => makeConnector(coinbaseApi, concurrencyContext)
    }
  }

  private def makeConnector(
    coinbaseApi: GenericExchangeApi,
    concurrencyContext: ConcurrencyContext,
  )(implicit ec: ExecutionContext) =
    new ExchangeConnector {

      private def makeCandleHistoryStream(tradingPair: TradingPair, candleLength: Duration) = {
        val segmentLoader: CandleHistorySegmentLoader =
          new CandleHistorySegmentLoader(
          batchLoader = start => coinbaseApi.getCandleBatch(tradingPair, candleLength, start),
          candleLength = candleLength,
          clock = SystemClock,
        )
        new PollingCandleHistoryStream(
          segmentLoader = segmentLoader.loadFrom,
          interval = FiniteDuration(candleLength.getSeconds / 2, "seconds"),
          retryInterval = FiniteDuration(10, "seconds"),
          updateOverlapStrategy = chs => chs.end.minusMillis(2 * candleLength.toMillis),
          sourceQueueFactory = concurrencyContext.sourceQueueFactory,
        )
      }

      override def candleHistoryStream(
        tradingPair: TradingPair,
        initialSegment: CandleHistorySegment,
      ): Source[CandleHistorySegment, NotUsed] =
        makeCandleHistoryStream(tradingPair, initialSegment.candleLength).source(initialSegment)

      private def makeTradeHistoryStream(tradingPair: TradingPair) = ???

      def tradeHistoryStream(
        tradingPair: TradingPair,
        initialSegment: TradeHistorySegment,
      ): Source[TradeHistorySegment, NotUsed] = ???

      override def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]] =
        coinbaseApi.sendTradeRequest(request)

      override def openOrdersStream(tradingPair: TradingPair): Source[Set[Order], NotUsed] = ???

    }

}
