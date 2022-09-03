package io.liquirium.connect

import io.liquirium.core.{Candle, CandleHistorySegment}

import java.time.{Duration, Instant}

case class CandleBatch(
  start: Instant,
  candleLength: Duration,
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
    if (c.length != candleLength)
      throw new RuntimeException("A candle with deviating length was encountered")
    if ((c.startTime.toEpochMilli - nextExpectedStart.toEpochMilli) % candleLength.toMillis != 0) {
      throw new RuntimeException("Candle start is not aligned with candle history segment candle length")
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
    if ((nextStart.toEpochMilli - start.toEpochMilli) % candleLength.toMillis != 0) {
      throw new RuntimeException("Next batch start is not aligned with candle batch candle length")
    }
  }

  def toHistorySegment: CandleHistorySegment = {
    val chs = candles.foldLeft(CandleHistorySegment.empty(start, candleLength)) {
      case (s, c) => s.padUntil(c.startTime).append(c)
    }
    chs.padUntil(nextBatchStart getOrElse chs.end)
  }

}
