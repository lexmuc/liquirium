package io.liquirium.connect.bitfinex

import java.time.Instant

case class TradeHistory(recentTrades: Seq[BitfinexTrade], start: Instant) {

  def min(i1: Instant, i2: Instant) = if (i1 isBefore i2) i1 else i2

  def max(i1: Instant, i2: Instant) = if (i1 isBefore i2) i2 else i1

  def dropBefore(target: Instant): TradeHistory = {
    val maxStart = recentTrades.lastOption.map(_.timestamp) getOrElse start
    val newStart = min(maxStart, max(start, target))

    TradeHistory(
      recentTrades.dropWhile(_.timestamp isBefore newStart),
      newStart
    )
  }

  def append(tt: Seq[BitfinexTrade]): TradeHistory = copy(recentTrades ++ tt)

}
