package io.liquirium.core

import io.liquirium.core.TradeHistorySegment.{Empty, Increment}

import java.time.Instant
import scala.annotation.tailrec
import scala.collection.SeqLike

trait TradeHistorySegment extends Seq[Trade] with SeqLike[Trade, Seq[Trade]] {

  val start: Instant

  def end: Instant

  protected def trades: IndexedSeq[Trade]

  def append(trade: Trade): TradeHistorySegment = {

    val lastTime = lastOption match {
      case None => start
      case Some(trade) => trade.time
    }

    if (trade.time isBefore lastTime)
      throw new RuntimeException("Trades must be in chronological order")

    if (trades.map(_.id).contains(trade.id))
      throw new RuntimeException("Trade ids must be unique")

    Increment(init = this, trade)
  }

  @tailrec
  final override def dropRight(n: Int): TradeHistorySegment = this match {
    case es: Empty => es
    case s: Increment => if (n == 0) s else s.init.dropRight(n - 1)
  }

  override def length: Int = trades.size

  override def iterator: Iterator[Trade] = trades.iterator

  override def apply(idx: Int): Trade = trades.apply(idx)

  def extendWith(other: TradeHistorySegment): TradeHistorySegment = {
    assertExtensionCompatibility(other)

    val thisTradesOverlap = trades.dropWhile(_.time isBefore other.start)
    val otherTradesOverlap = other.trades.takeWhile(!_.time.isAfter(end))

    val identicalTrades = thisTradesOverlap.zip(otherTradesOverlap).takeWhile {
      case (a, b) => a == b
    }.toList

    val thisUnchangedPart = identicalTrades match {
      case Nil => cutBefore(other.start)
      case _ => cutAfter(identicalTrades.reverse.head._1)
    }

    val newTrades: IndexedSeq[Trade] = identicalTrades match {
      case Nil => other.trades
      case _ => other.trades.reverse.takeWhile(_ != identicalTrades.reverse.head._1).reverse
    }

    newTrades.foldLeft(thisUnchangedPart) { (ths, trade) => ths.append(trade) }.cutAfter(other.last)
  }


  @tailrec
  private def cutBefore(time: Instant): TradeHistorySegment = this match {
    case Empty(_) => this
    case s: Increment => if (s.last.time.toEpochMilli >= time.toEpochMilli) s.init.cutBefore(time) else s
  }

  @tailrec
  private def cutAfter(trade: Trade): TradeHistorySegment = this match {
    case Empty(_) => this
    case s: Increment => if (s.last != trade) s.init.cutAfter(trade) else s
  }

  private def assertExtensionCompatibility(other: TradeHistorySegment): Unit = {
    if (other.start.isAfter(end))
      throw new RuntimeException("Extension segment may not start after segment being extended (no gaps allowed).")
  }

}


object TradeHistorySegment {

  def empty(start: Instant): TradeHistorySegment = Empty(start)

  def fromForwardTrades(start: Instant, trades: Iterable[Trade]): TradeHistorySegment = {

    trades.foldLeft(empty(start)) { (ths, trade) =>
      ths.append(trade)
    }
  }

  private case class Empty(
                            start: Instant,
                          ) extends TradeHistorySegment {
    override def end: Instant = start

    override def length: Int = 0

    override def lastOption: Option[Trade] = None

    override def iterator: Iterator[Trade] = Iterator.empty

    override protected def trades: IndexedSeq[Trade] = IndexedSeq[Trade]()
  }

  private case class Increment(
                                override val init: TradeHistorySegment,
                                override val last: Trade,
                              ) extends TradeHistorySegment {

    override def end: Instant = last.time

    override def lastOption: Option[Trade] = Some(last)

    override val start: Instant = init.start

    override protected def trades: IndexedSeq[Trade] = init.trades :+ last

  }

}