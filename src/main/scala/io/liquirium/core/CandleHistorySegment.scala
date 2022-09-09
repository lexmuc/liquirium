package io.liquirium.core

import java.time.{Duration, Instant}


trait CandleHistorySegment {

  def start: Instant

  def resolution: Duration

  def reverseCandles: List[Candle]

  def append(candle: Candle): CandleHistorySegment

  def end: Instant = reverseCandles.headOption.map(_.endTime) getOrElse start

  def padUntil(time: Instant): CandleHistorySegment =
    if (time isAfter end) append(Candle.empty(end, resolution)).padUntil(time) else this

  def extendWith(other: CandleHistorySegment): CandleHistorySegment

}


object CandleHistorySegment {

  def empty(start: Instant, resolution: Duration): CandleHistorySegment = Impl(start, resolution, List())

  def fromForwardCandles(start: Instant, resolution: Duration, candles: Iterable[Candle]): CandleHistorySegment =
    candles.foldLeft(empty(start, resolution)) {
      (chs, c) => chs.padUntil(c.startTime).append(c)
    }

  private case class Impl(
    start: Instant,
    resolution: Duration,
    reverseCandles: List[Candle],
  ) extends CandleHistorySegment {

    override def append(candle: Candle): CandleHistorySegment = {
      if (candle.startTime != end)
        throw new RuntimeException("Candles may only be appended at the end of a segment")
      if (candle.length != resolution)
        throw new RuntimeException("Candles may only be appended to segments with the same resolution")
      copy(reverseCandles = candle :: reverseCandles)
    }

    override def extendWith(other: CandleHistorySegment): CandleHistorySegment = {
      assertExtensionIsPossible(other)
      val oldForwardOverlap = reverseCandles.takeWhile(!_.startTime.isBefore(other.start)).reverse
      val otherForwardCandles = other.reverseCandles.reverse.dropWhile(_.startTime isBefore start)
      val matchSize = oldForwardOverlap.zip(otherForwardCandles).takeWhile {
        case (a, b) => a == b
      }.size
      val changedSize = oldForwardOverlap.size - matchSize
      val updatedAndNew = otherForwardCandles.drop(matchSize).reverse
      copy(reverseCandles = updatedAndNew ++ reverseCandles.drop(changedSize))
    }

    private def assertExtensionIsPossible(other: CandleHistorySegment): Unit = {
      if (other.start.isAfter(end))
        throw new RuntimeException("Cannot extend a candle history segment with another segment starting after the " +
          "one being extended ends.")
      if (other.resolution != resolution)
        throw new RuntimeException("Tried to extend a segment with another one having a different resolution")
      if (offsetMillis(other.start) != offsetMillis(start))
        throw new RuntimeException("Tried to extend a candle segment with another one having unaligned start")
    }

    private def offsetMillis(ts: Instant) = ts.toEpochMilli % resolution.toMillis

  }

}


