package io.liquirium.util.store

import io.liquirium.connect.TradeBatch
import io.liquirium.core.Trade

import java.time.Instant
import scala.concurrent.Future


trait TradeStore {

  def add(trades: Iterable[Trade]): Future[Unit]

  def get(from: Option[Instant] = None, until: Option[Instant] = None): Future[TradeBatch]

  def deleteFrom(time: Instant): Future[Unit]

}
