package io.liquirium.connect.bitfinex

import io.liquirium.core.HistoryEntry

import java.time.Instant

case class BitfinexCandle(
  timestamp: Instant,
  open: BigDecimal,
  close: BigDecimal,
  high: BigDecimal,
  low: BigDecimal,
  volume: BigDecimal,
) extends HistoryEntry {

  override def historyId: String = timestamp.toEpochMilli.toString

  override def historyTimestamp: Instant = timestamp

}
