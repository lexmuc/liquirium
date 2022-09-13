package io.liquirium.core

import io.liquirium.core.CandleHistorySegment.{Empty, Increment}

import java.time.{Duration, Instant}
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.collection.{SeqLike, mutable}


sealed trait CandleHistorySegment extends Seq[Candle] with SeqLike[Candle, Seq[Candle]] {

  def start: Instant

  def end: Instant

  def resolution: Duration

  protected def candles: IndexedSeq[Candle]

  def append(candle: Candle): CandleHistorySegment = {
    if (candle.startTime != end)
      throw new RuntimeException("Appended candles must start at the end of the segment.")
    if (candle.length != resolution)
      throw new RuntimeException("Appended candles must match the segment resolution.")
    Increment(init = this, candle)
  }

  @tailrec
  final override def dropRight(n: Int): CandleHistorySegment = this match {
    case es: Empty => es
    case s: Increment => if (n == 0) s else s.init.dropRight(n - 1)
  }

  override def length: Int = candles.size

  override def iterator: Iterator[Candle] = candles.iterator

  override def apply(idx: Int): Candle = candles.apply(idx)

  def padUntil(time: Instant): CandleHistorySegment =
    if (time isAfter end) append(Candle.empty(end, resolution)).padUntil(time) else this

  override def newBuilder: mutable.Builder[Candle, Seq[Candle]] =
    new ArrayBuffer[Candle].mapResult { b => b }

  def extendWith(other: CandleHistorySegment): CandleHistorySegment = {
    assertExtensionCompatibility(other)
    val newCandles = other
      .dropWhile(_.startTime isBefore start)
      .dropWhile(c => candles.contains(c))

    val unchangedPart = cutOff(newCandles.headOption.map(_.startTime) getOrElse this.end)
    newCandles.foldLeft(unchangedPart) { (chs, c) => chs.append(c) }.cutOff(other.end)
  }

  @tailrec
  private def cutOff(time: Instant): CandleHistorySegment = this match {
    case Empty(_, _) => this
    case s: Increment => if (s.end isAfter time) s.init.cutOff(time) else s
  }

  private def assertExtensionCompatibility(other: CandleHistorySegment): Unit = {
    if (other.start.isAfter(end))
      throw new RuntimeException("Extension segment is not consecutive or overlapping.")
    if (other.resolution != resolution)
      throw new RuntimeException("Extension segment has different resolution.")
    if (offsetMillis(other.start) != offsetMillis(start))
      throw new RuntimeException("Extension segment start is not properly aligned.")
  }

  private def offsetMillis(ts: Instant) = ts.toEpochMilli % resolution.toMillis

}


object CandleHistorySegment {

  def empty(start: Instant, resolution: Duration): CandleHistorySegment = Empty(start, resolution)

  def fromForwardCandles(start: Instant, resolution: Duration, candles: Iterable[Candle]): CandleHistorySegment =
    candles.foldLeft(empty(start, resolution)) {
      (chs, c) => chs.padUntil(c.startTime).append(c)
    }

  private case class Empty(
    start: Instant,
    resolution: Duration,
  ) extends CandleHistorySegment {
    override def end: Instant = start

    override def length: Int = 0

    override def lastOption: Option[Candle] = None

    override def iterator: Iterator[Candle] = Iterator.empty

    override protected def candles: IndexedSeq[Candle] = IndexedSeq[Candle]()
  }

  private case class Increment(
    override val init: CandleHistorySegment,
    override val last: Candle,
  ) extends CandleHistorySegment {

    override def end: Instant = last.endTime

    override def lastOption: Option[Candle] = Some(last)

    override val start: Instant = init.start

    override val resolution: Duration = init.resolution

    override protected def candles: IndexedSeq[Candle] = init.candles :+ last

  }

}


