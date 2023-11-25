package io.liquirium.connect

import io.liquirium.core.{Trade, TradeHistorySegment}

import java.time.Instant

case class TradeBatch(
  start: Instant,
  trades: Seq[Trade],
  nextBatchStart: Option[Instant],
) {

  if (trades.nonEmpty) {
    if (trades.head.time.isBefore(start))
      throw new RuntimeException("Trades in a batch must not be earlier than the batch start")

    trades.tail.foldLeft(trades.head) { (prev, t) =>
      if (prev.compareTo(t) >= 0)
        throw new RuntimeException("Trades in a batch must be ordered by time and id")
      t
    }
  }

  nextBatchStart.foreach { nbs =>
    if (!nbs.isAfter(start))
      throw new RuntimeException("Next trade batch start must come after the current batch start")
  }

  def toHistorySegment: TradeHistorySegment = TradeHistorySegment.fromForwardTrades(start, trades)

}
