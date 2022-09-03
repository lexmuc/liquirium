package io.liquirium.core

import java.time.{Duration, Instant}
import scala.annotation.tailrec

case class CandleHistorySegment(start: Instant, resolution: Duration, reverseCandles: List[Candle]) {

  def append(candle: Candle): CandleHistorySegment = {
    if (candle.startTime != end)
      throw new RuntimeException("Candles may only be appended at the end of a segment")
    if (candle.length != resolution)
      throw new RuntimeException("Candles may only be appended to segments with the same resolution")
    copy(reverseCandles = candle :: reverseCandles)
  }

  def extendWith(other: CandleHistorySegment): CandleHistorySegment = {
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
    if (other.startOffsetMillis != startOffsetMillis)
      throw new RuntimeException("Tried to extend a candle segment with another one having unaligned start")
  }

  def end: Instant = reverseCandles.headOption.map(_.endTime) getOrElse start

  private def startOffsetMillis: Long = start.toEpochMilli % resolution.toMillis

}

object CandleHistorySegment {

  def empty(start: Instant, resolution: Duration): CandleHistorySegment =
    CandleHistorySegment(start, resolution, List())

  def fromForwardCandles(
    start: Instant,
    resolution: Duration,
    candles: Iterable[Candle],
  ): CandleHistorySegment =
    CandleHistorySegment(
      start = start,
      resolution = resolution,
      reverseCandles = fillGapsAndReverse(candles, start, resolution),
    )

  private def fillGapsAndReverse(
    candles: Iterable[Candle],
    start: Instant,
    resolution: Duration,
  ): List[Candle] = {
    @tailrec
    def fillUntil(t: Instant, cc: List[Candle], nextSlotStart: Instant): List[Candle] =
      if (t == nextSlotStart) cc
      else fillUntil(t, Candle.empty(nextSlotStart, resolution) :: cc, nextSlotStart plus resolution)

    candles.foldLeft((List[Candle](), start)) { case ((cc, nextStart), c) =>
      checkCandle(c, nextStart, resolution)
      (c :: fillUntil(c.startTime, cc, nextStart), c.endTime)
    }._1
  }

  private def checkCandle(c: Candle, nextExpectedStart: Instant, resolution: Duration): Unit = {
    if (c.startTime.isBefore(nextExpectedStart))
      throw new RuntimeException("Candles are apparently not properly ordered")
    if (c.length != resolution)
      throw new RuntimeException("A candle with deviating length was encountered")
    if ((c.startTime.toEpochMilli - nextExpectedStart.toEpochMilli) % resolution.toMillis != 0)  {
      throw new RuntimeException("Candle start is not aligned with candle history segment resolution")
    }
  }

}
