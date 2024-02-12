package io.liquirium.util

import io.liquirium.util.akka.DefaultConcurrencyContext
import _root_.akka.actor.typed.DispatcherSelector
import io.liquirium.connect.ExchangeConnector
import io.liquirium.core.{CachingCandleHistoryLoader, CachingTradeHistoryLoader, CandleHistoryCache, CandleHistoryLoader, CandleHistorySegment, ExchangeId, Market, TradeHistoryCache, TradeHistoryLoader}

import java.nio.file.{Path, Paths}
import java.sql.{Connection, DriverManager}
import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

package object store {

  private implicit val executionContext: ExecutionContext =
    DefaultConcurrencyContext.actorSystem.dispatchers.lookup(DispatcherSelector.fromConfig("h2-request-dispatcher"))

  def h2candleStoreProvider(cacheDirectory: Path): CandleStoreProvider = {
    val cacheDirectoryString = cacheDirectory.toString
    new H2CandleStoreProvider((id, candleLength) => {
      val h2DbUrl = s"jdbc:h2:file:$cacheDirectoryString/candles/$id;TRACE_LEVEL_FILE=0"
      new H2CandleStore(DriverManager.getConnection(h2DbUrl), candleLength)
    })
  }

  def getCachingCandleHistoryLoaderProvider(
    getConnector: ExchangeId => Future[ExchangeConnector],
    cacheDirectory: Path,
  ): CandleHistoryLoaderProvider =
    new CandleHistoryLoaderProvider {
      override def getHistoryLoader(market: Market, resolution: Duration): Future[CandleHistoryLoader] = {
        for {
          connector <- getConnector(market.exchangeId)
        } yield getCachingCandleHistoryLoader(market, resolution, connector, cacheDirectory)
      }
    }

  def h2tradeStoreProvider(storePath: Path): TradeStoreProvider = new H2TradeStoreProvider((id, market) => {
    val connection = h2tradesConnectionProvider(storePath)(market)
    // #TODO. Not the only constructor call
    new H2TradeStore(connection, market)
  })

  private def h2tradesConnectionProvider(storePath: Path): Market => Connection = { market =>
    val marketString = s"${market.exchangeId.value}-${market.tradingPair.base}-${market.tradingPair.quote}"
    val pathString = storePath.toAbsolutePath.toString
    val h2DbUrl = s"jdbc:h2:file:$pathString/$marketString;TRACE_LEVEL_FILE=0"
    DriverManager.getConnection(h2DbUrl)
  }

  def getCachingCandleHistoryLoader(
    market: Market,
    candleLength: Duration,
    connector: ExchangeConnector,
    cacheDirectory: Path,
  ): CachingCandleHistoryLoader = {
    val cache = new CandleHistoryCache(h2candleStoreProvider(cacheDirectory).getStore(market, candleLength))
    val loader = new CandleHistoryLoader {
      override def load(start: Instant, end: Instant): Future[CandleHistorySegment] =
        connector.loadCandleHistory(market.tradingPair, candleLength, start, end)
    }
    new CachingCandleHistoryLoader(candleLength, loader, cache)
  }

  def getCachingTradeHistoryLoaderProvider(
    getConnector: ExchangeId => Future[ExchangeConnector],
  ): TradeHistoryLoaderProvider =
    new TradeHistoryLoaderProvider {
      override def getHistoryLoader(market: Market): Future[TradeHistoryLoader] =
        for {
          connector <- getConnector(market.exchangeId)
        } yield getCachingTradeHistoryLoader(market, connector)
    }

  def getCachingTradeHistoryLoader(
    market: Market,
    connector: ExchangeConnector,
  ): CachingTradeHistoryLoader = {
    val tradesPath = Paths.get("data/trades")
    val connection = h2tradesConnectionProvider(tradesPath)(market)
    val tradeStore = new H2TradeStore(connection, market)
    val startStore = new H2TradeSegmentStartStore(connection)

    val cache = new TradeHistoryCache(tradeStore, startStore)
    val liveLoader = connector.getTradeHistoryLoader(market.tradingPair)
    new CachingTradeHistoryLoader(liveLoader, cache, Duration.ofMinutes(10))
  }

}
