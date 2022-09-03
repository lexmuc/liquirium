package io.liquirium.connect.deribit

import io.liquirium.connect.deribit.DeribitDirection.Buy
import io.liquirium.core.HistoryEntry

import java.time.Instant

case class DeribitTrade(
  id: String,
  sequenceNumber: Long,
  direction: DeribitDirection,
  orderId: String,
  instrument: String,
  quantity: BigDecimal,
  price: BigDecimal,
  indexPrice: BigDecimal,
  fee: BigDecimal,
  feeCurrency: String,
  timestamp: Long,
) extends HistoryEntry {

  override def historyId: String = id

  override def historyTimestamp: Instant = Instant.ofEpochMilli(timestamp)

  def volume: BigDecimal = quantity * price

  def isFutureTrade: Boolean = instrument.count(_ == '-') == 1

  def positionChange: BigDecimal = if (direction == Buy) quantity else -quantity

}
