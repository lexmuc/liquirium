package io.liquirium.core

import java.time.Instant

case class TradeHistorySegment(start: Instant, reverseTrades: List[Trade]) {

  //  def inject(trade: Trade): TradeHistorySegment = {
  //    if (trade.time.isBefore(start) || trade.time.isAfter(end))
  //      throw new RuntimeException("Trades may only be injected between start and end of a segment ???")
  //    ???
  //  }

  def extendWith(other: TradeHistorySegment): TradeHistorySegment = {
    assertExtensionIsPossible(other)
    val thisTrades = reverseTrades.dropWhile(!_.time.isBefore(other.start))
    val otherTrades = other.reverseTrades.takeWhile(!_.time.isBefore(start))
    copy(reverseTrades = otherTrades ++ thisTrades)
  }

  private def assertExtensionIsPossible(other: TradeHistorySegment): Unit = {
    if (other.start.isAfter(end))
      throw new RuntimeException("Extension segment may not start after segment being extended (no gaps allowed).")
  }

  def end: Instant = reverseTrades.headOption.map(_.time) getOrElse start

}

object TradeHistorySegment {

  def empty(start: Instant): TradeHistorySegment = TradeHistorySegment(start, List())

  def fromForwardTrades(start: Instant, trades: Iterable[Trade]): TradeHistorySegment =
    TradeHistorySegment(start = start, reverse(trades, start))

  private def reverse(trades: Iterable[Trade], start: Instant): List[Trade] = {

    assertIdsAreUnique(trades)

    trades.foldLeft((List[Trade](), start)) {
      case ((tt, previousTime), t) =>
        checkTrade(t, previousTime)
        (t :: tt, t.time)
    }._1
  }

  private def checkTrade(trade: Trade, nextTime: Instant): Unit = {
    if (trade.time.isBefore(nextTime))
      throw new RuntimeException("Trades are apparently not properly ordered")
  }

  private def assertIdsAreUnique(trades: Iterable[Trade]): Unit = {
    val idSet = trades.map(_.id).toSet
    if (idSet.size != trades.size)
      throw new RuntimeException("Trade ids must be unique")
  }

}
