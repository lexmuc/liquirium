package io.liquirium.util

import io.liquirium.util.akka.DefaultConcurrencyContext
import _root_.akka.actor.typed.DispatcherSelector

import java.nio.file.{Path, Paths}
import java.sql.DriverManager
import scala.concurrent.ExecutionContext

package object store {

  private val workingDir = Paths.get("").toAbsolutePath.toString

  private implicit val executionContext: ExecutionContext =
    DefaultConcurrencyContext.actorSystem.dispatchers.lookup(DispatcherSelector.fromConfig("h2-request-dispatcher"))

  val h2candleStoreProvider: CandleStoreProvider = new H2CandleStoreProvider((id, candleLength) => {
    val h2DbUrl = s"jdbc:h2:file:$workingDir/data/candles/$id;TRACE_LEVEL_FILE=0"
    new H2CandleStore(DriverManager.getConnection(h2DbUrl), candleLength)
  })

  def h2tradeStoreProvider(storePath: Path): TradeStoreProvider = new H2TradeStoreProvider((id, market) => {
    val pathString = storePath.toAbsolutePath.toString
    val h2DbUrl = s"jdbc:h2:file:$pathString/$id;TRACE_LEVEL_FILE=0"
    new H2TradeStore(DriverManager.getConnection(h2DbUrl), market)
  })

}