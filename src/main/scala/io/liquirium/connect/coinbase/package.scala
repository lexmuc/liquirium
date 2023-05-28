package io.liquirium.connect

import io.liquirium.core.ExchangeId
import io.liquirium.util.akka._
import io.liquirium.util.{ApiCredentials, SystemClock}

import scala.concurrent.duration.DurationInt
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


}