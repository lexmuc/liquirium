package io.liquirium.connect

import io.liquirium.core.ExchangeId
import io.liquirium.util.akka._
import io.liquirium.util.{ApiCredentials, SystemClock}

import scala.concurrent.duration.DurationInt
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

}
