package io.liquirium.connect

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.connect.binance.BinanceRestApi.BinanceApiRequest
import io.liquirium.core.{CandleHistorySegment, ExchangeId, TradingPair}
import io.liquirium.util.akka._
import io.liquirium.util.{ApiCredentials, SystemClock}

import java.time.Duration
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

  def connector(
    concurrencyContext: ConcurrencyContext,
    credentials: ApiCredentials,
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

      private def makeCandleHistoryStream(tradingPair: TradingPair, resolution: Duration) = {
        val segmentLoader = new CandleHistorySegmentLoader(
          batchLoader = start => binanceApi.getForwardCandleBatch(tradingPair, resolution, start)
        )
        new PollingCandleHistoryStream(
          segmentLoader = segmentLoader.loadFrom,
          interval = FiniteDuration(resolution.getSeconds / 2, "seconds"),
          retryInterval = FiniteDuration(10, "seconds"),
          updateStartProvider = chs => chs.end.minusMillis(resolution.toMillis),
          sourceQueueFactory = concurrencyContext.sourceQueueFactory,
        )
      }

      override def candleHistoryStream(
        tradingPair: TradingPair,
        initialSegment: CandleHistorySegment,
      ): Source[CandleHistorySegment, NotUsed] =
        makeCandleHistoryStream(tradingPair, initialSegment.resolution).source(initialSegment)
    }

}
