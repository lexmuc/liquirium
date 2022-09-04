package io.liquirium.connect.binance

import io.liquirium.core.HistoryEntry

import java.time.Instant

case class BinanceCandle(
  openTime: Instant,
  open: BigDecimal,
  high: BigDecimal,
  low: BigDecimal,
  close: BigDecimal,
  quoteAssetVolume: BigDecimal,
  closeTime: Instant
) extends HistoryEntry {
  override def historyId: String = openTime.toEpochMilli.toString

  override def historyTimestamp: Instant = openTime
}
