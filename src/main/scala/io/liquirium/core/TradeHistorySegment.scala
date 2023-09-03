package io.liquirium.core

import io.liquirium.core.TradeHistorySegment.{Empty, Increment}
import io.liquirium.eval.BasicIncrementalValue

import java.time.Instant
import scala.annotation.tailrec
import scala.collection.SeqLike

trait TradeHistorySegment
  extends Seq[Trade] with
    SeqLike[Trade, Seq[Trade]] with
    BasicIncrementalValue[Trade, TradeHistorySegment] {

  val start: Instant

  def end: Instant

  protected def trades: IndexedSeq[Trade]

  protected def tradeIds: Set[TradeId]

  def append(trade: Trade): TradeHistorySegment = {
    if (trade.time isBefore start)
      throw new RuntimeException("Appended trades must not be earlier than the segment start")

    if (lastOption.map(_.time).exists(_.isAfter(trade.time))) {
      throw new RuntimeException("Trades must be in chronological order")
    }

    if (tradeIds.contains(trade.id))
      throw new RuntimeException("Trade ids must be unique")

    Increment(init = this, trade)
  }

  override def inc(increment: Trade): TradeHistorySegment = append(increment)

  @tailrec
  final override def dropRight(n: Int): TradeHistorySegment = this match {
    case es: Empty => es
    case s: Increment => if (n == 0) s else s.init.dropRight(n - 1)
  }

  override def length: Int = trades.size

  override def iterator: Iterator[Trade] = trades.iterator

  override def apply(idx: Int): Trade = trades.apply(idx)

  @tailrec
  final def truncate(instant: Instant): TradeHistorySegment =
    if (instant isAfter end) this
    else this match {
      case _: Empty => this
      case s: Increment => s.init.truncate(instant)
    }

  def extendWith(other: TradeHistorySegment): TradeHistorySegment = {
    assertExtensionCompatibility(other)

    val ownOverlappingTrades = trades.reverseIterator.takeWhile(!_.time.isBefore(other.start)).toSeq.reverse
    val relevantOtherTrades = other.trades.dropWhile(_.time.isBefore(start))

    val identicalTrades = ownOverlappingTrades.zip(relevantOtherTrades).takeWhile { case (a, b) => a == b }
    val unchangedPart = this.dropRight(ownOverlappingTrades.size - identicalTrades.size)
    val newTrades = relevantOtherTrades.drop(identicalTrades.size)
    newTrades.foldLeft(unchangedPart) { (ths, trade) => ths.append(trade) }
  }

  def getExtensionTo(other: TradeHistorySegment): TradeHistorySegment = {
    if (other.root != root)
      throw new RuntimeException("An extension to another trade history segment can only be obtained when it has" +
        "the same root.")
    latestCommonAncestor(other) match {
      case Some(lca) =>
        val lastInstantTrades = lca.reverseIterator.takeWhile(_.time == lca.end).toSeq.reverse
        val allExtensionTrades = lastInstantTrades ++ other.incrementsAfter(lca)
        TradeHistorySegment.fromForwardTrades(lca.end, allExtensionTrades)
    }
  }

  private def assertExtensionCompatibility(other: TradeHistorySegment): Unit =
    if (other.start.isAfter(end))
      throw new RuntimeException("Extension segment may not start after segment being extended (no gaps allowed).")

}


object TradeHistorySegment {

  def empty(start: Instant): TradeHistorySegment = Empty(start)

  def fromForwardTrades(start: Instant, trades: Iterable[Trade]): TradeHistorySegment =
    trades.foldLeft(empty(start)) { (ths, trade) =>
      ths.append(trade)
    }

  private case class Empty(
    start: Instant,
  ) extends TradeHistorySegment {
    override def end: Instant = start

    override def length: Int = 0

    override def lastOption: Option[Trade] = None

    override def iterator: Iterator[Trade] = Iterator.empty

    override protected val trades: IndexedSeq[Trade] = IndexedSeq[Trade]()

    override protected val tradeIds: Set[TradeId] = Set()

    override def prev: Option[TradeHistorySegment] = None

    override def lastIncrement: Option[Trade] = None

  }

  private case class Increment(
    override val init: TradeHistorySegment,
    override val last: Trade,
  ) extends TradeHistorySegment {

    override def end: Instant = last.time

    override def lastOption: Option[Trade] = Some(last)

    override val start: Instant = init.start

    override protected val trades: IndexedSeq[Trade] = init.trades :+ last

    override protected val tradeIds: Set[TradeId] = init.tradeIds + last.id

    override def prev: Option[TradeHistorySegment] = Some(init)

    override def lastIncrement: Option[Trade] = Some(last)

  }

}