package io.liquirium.connect

import io.liquirium.core.{Candle, CandleHistorySegment}

import java.time.{Duration, Instant}

case class ForwardCandleBatch(
  start: Instant,
  resolution: Duration,
  candles: Iterable[Candle],
  nextBatchStart: Option[Instant],
) {

  candles.foldLeft(start) { case (s, c) =>
    checkCandle(c, s)
    c.endTime
  }
  nextBatchStart.foreach(checkNextStart)

  private def checkCandle(c: Candle, nextExpectedStart: Instant): Unit = {
    if (c.startTime.isBefore(nextExpectedStart))
      throw new RuntimeException("Candles are not properly ordered or start before the batch start")
    if (c.length != resolution)
      throw new RuntimeException("A candle with deviating length was encountered")
    if ((c.startTime.toEpochMilli - nextExpectedStart.toEpochMilli) % resolution.toMillis != 0) {
      throw new RuntimeException("Candle start is not aligned with candle history segment resolution")
    }
    nextBatchStart match {
      case Some(nbs) if !c.startTime.isBefore(nbs) =>
        throw new RuntimeException("Batch contains a candle of the next batch")
      case _ => ()
    }
  }

  private def checkNextStart(nextStart: Instant): Unit = {
    if (!nextStart.isAfter(start)) {
      throw new RuntimeException(s"Next batch start $nextStart is not later than start of the current batch")
    }
    if ((nextStart.toEpochMilli - start.toEpochMilli) % resolution.toMillis != 0) {
      throw new RuntimeException("Next batch start is not aligned with candle batch resolution")
    }
  }

  def toHistorySegment: CandleHistorySegment = {
    val chs = CandleHistorySegment.fromForwardCandles(
      start = start,
      resolution = resolution,
      candles = candles,
    )
    chs.padUntil(nextBatchStart getOrElse chs.end)
  }

}
