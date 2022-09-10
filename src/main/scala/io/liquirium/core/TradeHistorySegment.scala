package io.liquirium.core

import java.time.Instant

trait TradeHistorySegment {
  val start: Instant
  val reverseTrades: List[Trade]

  def extendWith(other: TradeHistorySegment) :TradeHistorySegment
  def append(trade: Trade): TradeHistorySegment
  def end: Instant
}


object TradeHistorySegment {

  def empty(start: Instant): TradeHistorySegment = Impl(start, List())

  private case class Impl(start: Instant, reverseTrades: List[Trade]) extends TradeHistorySegment {

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
        ths.append(trade)
      }
    }

    private def assertExtensionIsPossible(other: TradeHistorySegment): Unit = {
      if (other.start.isAfter(end))
        throw new RuntimeException("Extension segment may not start after segment being extended (no gaps allowed).")
    }

    def append(trade: Trade): Impl = {
      val lastTime = reverseTrades.headOption match {
        case None => start
        case Some(trade) => trade.time
      }

      if (trade.time isBefore lastTime)
        throw new RuntimeException("Trades must be in chronological order")

      if (reverseTrades.map(_.id).contains(trade.id))
        throw new RuntimeException("Trade ids must be unique")

      copy(reverseTrades = trade :: reverseTrades)
    }

    def end: Instant = reverseTrades.headOption.map(_.time) getOrElse start

  }

  def fromForwardTrades(start: Instant, trades: Iterable[Trade]): TradeHistorySegment = {

    trades.foldLeft(empty(start)) { (ths, trade) =>
      ths.append(trade)
    }
  }

}