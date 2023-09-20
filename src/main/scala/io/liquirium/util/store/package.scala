package io.liquirium.util

import io.liquirium.util.akka.DefaultConcurrencyContext
import _root_.akka.actor.typed.DispatcherSelector
import io.liquirium.connect.ExchangeConnector
import io.liquirium.core.{CachingCandleHistoryLoader, CandleHistoryCache, CandleHistoryLoader, CandleHistorySegment, ExchangeId, Market}

import java.nio.file.{Path, Paths}
import java.sql.DriverManager
import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

package object store {

  private val workingDir = Paths.get("").toAbsolutePath.toString

  private implicit val executionContext: ExecutionContext =
    DefaultConcurrencyContext.actorSystem.dispatchers.lookup(DispatcherSelector.fromConfig("h2-request-dispatcher"))

  val h2candleStoreProvider: CandleStoreProvider = new H2CandleStoreProvider((id, candleLength) => {
    val h2DbUrl = s"jdbc:h2:file:$workingDir/data/candles/$id;TRACE_LEVEL_FILE=0"
    new H2CandleStore(DriverManager.getConnection(h2DbUrl), candleLength)
  })

  def getCachingCandleHistoryLoaderProvider(
    getConnector: ExchangeId => Future[ExchangeConnector],
  ): CandleHistoryLoaderProvider =
    new CandleHistoryLoaderProvider {
      override def getHistoryLoader(market: Market, resolution: Duration): Future[CandleHistoryLoader] = {
        for {
          connector <- getConnector(market.exchangeId)
        } yield getCachingCandleHistoryLoader(market, resolution, connector)
      }
    }

  def h2tradeStoreProvider(storePath: Path): TradeStoreProvider = new H2TradeStoreProvider((id, market) => {
    val pathString = storePath.toAbsolutePath.toString
    val h2DbUrl = s"jdbc:h2:file:$pathString/$id;TRACE_LEVEL_FILE=0"
    new H2TradeStore(DriverManager.getConnection(h2DbUrl), market)
  })

  def getCachingCandleHistoryLoader(
    market: Market,
    candleLength: Duration,
    connector: ExchangeConnector,
  ): CachingCandleHistoryLoader = {
    val cache = new CandleHistoryCache(h2candleStoreProvider.getStore(market, candleLength))
    val loader = new CandleHistoryLoader {
      override def load(start: Instant, end: Instant): Future[CandleHistorySegment] =
        connector.loadCandleHistory(market.tradingPair, candleLength, start, end)
    }
    new CachingCandleHistoryLoader(candleLength, loader, cache)
  }

  def getTradeHistoryProviderWithLiveUpdate(
    market: Market,
    connector: ExchangeConnector,
    overlapDuration: Duration = Duration.ofMinutes(10),
  ): StoreBasedTradeHistoryLoaderWithOnDemandUpdate = {
    val historyStore = new TradeHistoryStore(h2tradeStoreProvider(Paths.get("data/trades")).getStore(market))
    new StoreBasedTradeHistoryLoaderWithOnDemandUpdate(
      baseStore = historyStore,
      liveSegmentLoader = start => connector.loadTradeHistory(market.tradingPair, start),
      overlapDuration = overlapDuration,
    )
  }

}
