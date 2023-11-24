package io.liquirium.connect.poloniex

import io.liquirium.core.HistoryEntry

import java.time.Instant

/**
 * @param low              lowest price over the interval
 * @param high             highest price over the interval
 * @param open             price at the start time
 * @param close            price at the end time
 * @param amount           quote units traded over the interval
 * @param quantity         base units traded over the interval
 * @param buyTakerAmount   quote units traded over the interval filled by market buy orders
 * @param buyTakerQuantity base units traded over the interval filled by market buy orders
 * @param tradeCount       count of trades
 * @param ts               time the record was pushed
 * @param weightedAverage  weighted average over the interval
 * @param interval         the selected interval
 * @param startTime        start time of interval
 * @param closeTime        close time of interval
 */

case class PoloniexCandle(
  low: BigDecimal,
  high: BigDecimal,
  open: BigDecimal,
  close: BigDecimal,
  amount: BigDecimal,
  quantity: BigDecimal,
  buyTakerAmount: BigDecimal,
  buyTakerQuantity: BigDecimal,
  tradeCount: Int,
  ts: Instant,
  weightedAverage: BigDecimal,
  interval: PoloniexCandleLength,
  startTime: Instant,
  closeTime: Instant,
) extends HistoryEntry {

  override def historyId: String = startTime.toEpochMilli.toString

  override def historyTimestamp: Instant = startTime

}

