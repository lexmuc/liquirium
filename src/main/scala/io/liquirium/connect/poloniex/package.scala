package io.liquirium.connect

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.core._
import io.liquirium.util.akka._
import io.liquirium.util.{ApiCredentials, SystemClock}

import java.time.{Duration, Instant}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

package object poloniex {

  val exchangeId: ExchangeId = ExchangeId("POLONIEX")

  type AsyncPoloniexApi = AsyncApi[PoloniexApiRequest[_]]

  private val jsonConverter = new PoloniexJsonConverter()

  private val modelConverter = new PoloniexModelConverter(exchangeId)

  private val apiLogger = io.liquirium.util.Logger("poloniex-api")

  private def authenticator(credentials: ApiCredentials) =
    new PoloniexAuthenticator(credentials, SystemClock)

  def poloniexHttpService(
    concurrencyContext: ConcurrencyContext,
    authenticator: PoloniexAuthenticator,
  ): PoloniexHttpService =
    new PoloniexHttpService(
      concurrencyContext.asyncHttpService,
      authenticator,
      PoloniexResponseTransformer,
      apiLogger
    )(
      concurrencyContext.executionContext
    )

  def asyncWrappedRestApi(
    concurrencyContext: ConcurrencyContext,
    credentials: ApiCredentials,
  ): Future[AsyncPoloniexApi] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    val spawner = concurrencyContext.spawner
    val extendedHttpService = poloniexHttpService(concurrencyContext, authenticator(credentials))
    val coreApi = new PoloniexRestApi(extendedHttpService, jsonConverter, apiLogger)
    for {
      coreActor <- spawner.spawnAsync(
        AsyncApiAdapter.actorBasedApi(coreApi),
        "poloniex-core-api",
      )
      throttler <- spawner.spawnAsync(
        AsyncRequestThrottler.behavior(coreActor, 200.millis),
        "poloniex-request-throttler",
      )
      sequencer <- spawner.spawnAsync(
        AsyncRequestSequencer.forActor(throttler),
        "poloniex-request-sequencer",
      )
    } yield AsyncApiAdapter.futureBasedApi(sequencer)(concurrencyContext.actorSystem)
  }

  def api(concurrencyContext: ConcurrencyContext, credentials: ApiCredentials): Future[GenericExchangeApi] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    for {
      restApi <- asyncWrappedRestApi(concurrencyContext, credentials)
    } yield new PoloniexApiAdapter(restApi, modelConverter)
  }

  def getConnector(
    concurrencyContext: ConcurrencyContext = DefaultConcurrencyContext,
    credentials: ApiCredentials = ApiCredentials("", ""),
  ): Future[ExchangeConnector] = {
    implicit val ec: ExecutionContext = concurrencyContext.executionContext
    api(concurrencyContext, credentials).map {
      poloniexApi => makeConnector(poloniexApi, concurrencyContext)
    }
  }

  private def makeConnector(
    poloniexApi: GenericExchangeApi,
    concurrencyContext: ConcurrencyContext,
  )(implicit ec: ExecutionContext) =
    new ExchangeConnector {

      private def makeCandleHistorySegmentLoader(
        tradingPair: TradingPair,
        candleLength: Duration,
      ): LiveCandleHistoryLoader =
        new LiveCandleHistoryLoader(
          start => poloniexApi.getCandleBatch(tradingPair, candleLength, start),
          candleLength = candleLength,
        )

      override def loadCandleHistory(
        tradingPair: TradingPair,
        duration: Duration,
        start: Instant,
      ): Future[CandleHistorySegment] =
        makeCandleHistorySegmentLoader(tradingPair, duration).load(start, SystemClock.getTime)

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

      override def candleHistoryStream(
        tradingPair: TradingPair,
        initialSegment: CandleHistorySegment,
      ): Source[CandleHistorySegment, NotUsed] =
        makeCandleHistoryStream(tradingPair, initialSegment.candleLength).source(initialSegment)

      private def makeTradeHistorySegmentLoader(pair: TradingPair) =
        new TradeHistorySegmentLoader(start => poloniexApi.getTradeBatch(pair, start))

      override def loadTradeHistory(tradingPair: TradingPair, start: Instant): Future[TradeHistorySegment] =
        makeTradeHistorySegmentLoader(tradingPair).loadFrom(start)

      private def makeTradeHistoryStream(tradingPair: TradingPair) = ???

      def tradeHistoryStream(
        tradingPair: TradingPair,
        initialSegment: TradeHistorySegment,
      ): Source[TradeHistorySegment, NotUsed] = ???

      override def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]] =
        poloniexApi.sendTradeRequest(request)

      override def openOrdersStream(tradingPair: TradingPair): Source[Set[Order], NotUsed] = ???

    }

}
