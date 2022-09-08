package io.liquirium.core

import io.liquirium.core.TradeHistorySegment.append

import java.time.Instant


object TradeHistorySegment {

  def empty(start: Instant): TradeHistorySegment = TradeHistorySegment(start, List())

  def fromForwardTrades(start: Instant, trades: Iterable[Trade]): TradeHistorySegment = {

    trades.foldLeft(empty(start)) { (ths, trade) =>
      append(ths, trade)
    }
  }

  private def append(ths: TradeHistorySegment, trade: Trade): TradeHistorySegment = {
    val lastTime = ths.reverseTrades.headOption match {
      case None => ths.start
      case Some(trade) => trade.time
    }

    if (trade.time isBefore lastTime)
      throw new RuntimeException("Trades must be in chronological order")

    if (ths.reverseTrades.map(_.id).contains(trade.id))
      throw new RuntimeException("Trade ids must be unique")

    ths.copy(reverseTrades = trade :: ths.reverseTrades)
  }
}

case class TradeHistorySegment(start: Instant, reverseTrades: List[Trade]) {

  //  def inject(trade: Trade): TradeHistorySegment = {
  //    if (trade.time.isBefore(start) || trade.time.isAfter(end))
  //      throw new RuntimeException("Trades may only be injected between start and end of a segment ???")
  //    ???
  //  }

  def extendWith(other: TradeHistorySegment): TradeHistorySegment = {
    assertExtensionIsPossible(other)

    val oldForwardOverlap = reverseTrades.takeWhile(!_.time.isBefore(other.start)).reverse
    val otherForwardTrades = other.reverseTrades.reverse.dropWhile(_.time isBefore start)

    val matchSize = oldForwardOverlap.zip(otherForwardTrades).takeWhile {
      case (a, b) => a == b
    }.size

    val changedSize = oldForwardOverlap.size - matchSize
    val newReverseTrades = otherForwardTrades.drop(matchSize).reverse
    val oldReverseTrades = reverseTrades.drop(changedSize)
    val mergedTrades = newReverseTrades ++ oldReverseTrades

    mergedTrades.foldRight(copy(reverseTrades = List[Trade]())) { (trade, ths) =>
      append(ths, trade)
    }
  }

  private def assertExtensionIsPossible(other: TradeHistorySegment): Unit = {
    if (other.start.isAfter(end))
      throw new RuntimeException("Extension segment may not start after segment being extended (no gaps allowed).")
  }

  def end: Instant = reverseTrades.headOption.map(_.time) getOrElse start

}


