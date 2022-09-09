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

    override def append(candle: Candle): Impl = {
      if (candle.startTime != end)
        throw new RuntimeException("Appended candles must start at the end of the segment.")
      if (candle.length != resolution)
        throw new RuntimeException("Appended candles must match the segment resolution.")
      copy(reverseCandles = candle :: reverseCandles)
    }

    override def extendWith(other: CandleHistorySegment): Impl = {
      assertExtensionCompatibility(other)
      val newCandles = other
        .reverseCandles.reverse
        .dropWhile(_.startTime isBefore start)
        .dropWhile(c => reverseCandles.contains(c))

      val unchangedPart = cutOff(newCandles.headOption.map(_.startTime) getOrElse this.end)
      newCandles.foldLeft(unchangedPart) { (chs, c) => chs.append(c) }.cutOff(other.end)
    }

    private def cutOff(time: Instant): Impl =
      copy(reverseCandles = reverseCandles.dropWhile(_.endTime isAfter time))

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

}


