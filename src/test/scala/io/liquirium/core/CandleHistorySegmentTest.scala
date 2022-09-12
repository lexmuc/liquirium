package io.liquirium.core

import io.liquirium.core.helper.BasicTest

import java.time.{Duration, Instant}

class CandleHistorySegmentTest extends BasicTest {

  protected def fromForwardCandles(
    start: Instant,
    resolution: Duration,
  )(
    cc: Candle*
  ): CandleHistorySegment =
    CandleHistorySegment.fromForwardCandles(start, resolution, cc)

  protected def empty(start: Instant, resolution: Duration): CandleHistorySegment =
    CandleHistorySegment.empty(start, resolution)

}
