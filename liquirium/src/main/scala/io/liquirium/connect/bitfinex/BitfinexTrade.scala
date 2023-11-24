package io.liquirium.connect.bitfinex

import io.liquirium.core.HistoryEntry

import java.time.Instant

case class BitfinexTrade
(
  id: Long,
  symbol: String,
  timestamp: Instant,
  orderId: Long,
  amount: BigDecimal,
  price: BigDecimal,
  fee: BigDecimal,
  feeCurrency: String
) extends HistoryEntry {

  override def historyId: String = id.toString

  override def historyTimestamp: Instant = timestamp

}
