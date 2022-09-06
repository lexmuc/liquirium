package io.liquirium.core

import io.liquirium.core.helper.BasicTest

import java.time.{Duration, Instant}

class CandleHistorySegmentTest extends BasicTest {

  protected def fromForwardCandles(
    start: Instant,
    resolution: Duration,
    end: Option[Instant] = None,
  )(
    cc: Candle*
  ): CandleHistorySegment =
    CandleHistorySegment.fromForwardCandles(start, resolution, cc, end)

  protected def empty(start: Instant, resolution: Duration): CandleHistorySegment =
    CandleHistorySegment.empty(start, resolution)

}
