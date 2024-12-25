package io.liquirium.core

import io.liquirium.core.CandleHistorySegment.{Empty, Increment}
import io.liquirium.eval.BasicIncrementalValue

import java.time.{Duration, Instant}
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.collection.{SeqLike, mutable}


sealed trait CandleHistorySegment
  extends Seq[Candle]
    with SeqLike[Candle, Seq[Candle]]
    with BasicIncrementalValue[Candle, CandleHistorySegment] {

  def start: Instant

  def end: Instant

  def candleLength: Duration

  protected def candles: IndexedSeq[Candle]

  lazy val lastPrice: Option[BigDecimal] = {
    val it = reverseIterator.filter(!_.isEmpty).map(_.close)
    if (it.hasNext) Some(it.next()) else None
  }

  override def prev: Option[CandleHistorySegment] = this match {
    case _: Empty => None
    case Increment(init, _) => Some(init)
  }

  override def lastIncrement: Option[Candle] = this match {
    case _: Empty => None
    case Increment(_, c) => Some(c)
  }

  override def inc(increment: Candle): CandleHistorySegment = append(increment)

  def append(candle: Candle): CandleHistorySegment = {
    if (candle.startTime != end)
      throw new RuntimeException("Appended candles must start at the end of the segment.")
    if (candle.length != candleLength) {
      val details = s"Expected $candleLength but got ${candle.length}."
      throw new RuntimeException("Appended candles must match the segment candleLength. " + details)
    }
    Increment(init = this, candle)
  }

  @tailrec
  final override def dropRight(n: Int): CandleHistorySegment = this match {
    case es: Empty => es
    case s: Increment => if (n == 0) s else s.init.dropRight(n - 1)
  }

  override def length: Int = candles.size

  override def iterator: Iterator[Candle] = candles.iterator

  override def reverseIterator: Iterator[Candle] = candles.reverseIterator

  override def apply(idx: Int): Candle = candles.apply(idx)

  def padUntil(time: Instant): CandleHistorySegment =
    if (!(time isBefore  end.plus(candleLength))) append(Candle.empty(end, candleLength)).padUntil(time) else this

//  override def newBuilder: mutable.Builder[Candle, Seq[Candle]] = {
//    // Use ArrayBuffer as the underlying builder
//    new mutable.Builder[Candle, Seq[Candle]] {
//      private val buffer = ArrayBuffer.empty[Candle]
//
//      // Add a single element to the builder
//      override def +=(elem: Candle): this.type = {
//        buffer += elem
//        this
//      }
//
//      // Clear the builder
//      override def clear(): Unit = {
//        buffer.clear()
//      }
//
//      // Produce the final collection (convert to Seq)
//      override def result(): Seq[Candle] = buffer.toSeq
//    }
//  }

//  override def newBuilder: mutable.Builder[Candle, Seq[Candle]] = {
//
//    val buf = new ArrayBuffer[Candle]()
//    buf += candles
//    buf
//  }
  //    new ArrayBuffer[Candle].mapResult { b => b.toSeq }

  def extendWith(other: CandleHistorySegment): CandleHistorySegment = {
    assertExtensionCompatibility(other)
    val newCandles = other
      .dropWhile(_.startTime isBefore start)
      .dropWhile(c => candles.contains(c))

    val unchangedPart = truncate(newCandles.headOption.map(_.startTime) getOrElse this.end)
    newCandles.foldLeft(unchangedPart) { (chs, c) => chs.append(c) }.truncate(other.end)
  }

  @tailrec
  final def truncate(time: Instant): CandleHistorySegment = this match {
    case Empty(_, _) => this
    case s: Increment => if (s.end isAfter time) s.init.truncate(time) else s
  }

  private def assertExtensionCompatibility(other: CandleHistorySegment): Unit = {
    if (other.start.isAfter(end))
      throw new RuntimeException("Extension segment is not consecutive or overlapping.")
    if (other.candleLength != candleLength)
      throw new RuntimeException("Extension segment has different candleLength.")
    if (offsetMillis(other.start) != offsetMillis(start))
      throw new RuntimeException("Extension segment start is not properly aligned.")
  }

  private def offsetMillis(ts: Instant) = ts.toEpochMilli % candleLength.toMillis

  override def equals(that: Any): Boolean = that match {
    case that: CandleHistorySegment =>
      (that eq this.asInstanceOf[CandleHistorySegment]) ||
        (
          that.start == this.start &&
            that.candleLength == this.candleLength &&
            (that.reverseIterator sameElements this.reverseIterator)
          )
    case _ => super.equals(that)
  }

}


object CandleHistorySegment {

  def empty(start: Instant, candleLength: Duration): CandleHistorySegment = Empty(start, candleLength)

  def fromCandles(candles: Iterable[Candle]): CandleHistorySegment =
    candles.headOption match {
      case None => throw new RuntimeException("Cannot create CandleHistorySegment from empty Iterable")
      case Some(h) => candles.foldLeft(empty(h.startTime, h.length))(_.inc(_))
    }

  private case class Empty(
    start: Instant,
    candleLength: Duration,
  ) extends CandleHistorySegment {
    override def end: Instant = start

    override val length: Int = 0

    override def lastOption: Option[Candle] = None

    override def iterator: Iterator[Candle] = Iterator.empty

    override protected val candles: IndexedSeq[Candle] = IndexedSeq[Candle]()
  }

  private case class Increment(
    override val init: CandleHistorySegment,
    override val last: Candle,
  ) extends CandleHistorySegment {

    override def end: Instant = last.endTime

    override def lastOption: Option[Candle] = Some(last)

    override val start: Instant = init.start

    override val candleLength: Duration = init.candleLength

    override protected val candles: IndexedSeq[Candle] = init.candles :+ last

  }

}


