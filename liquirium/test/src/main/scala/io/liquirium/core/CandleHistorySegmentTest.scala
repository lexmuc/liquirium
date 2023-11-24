package io.liquirium.core

import io.liquirium.core.helpers.BasicTest

import java.time.{Duration, Instant}

class CandleHistorySegmentTest extends BasicTest {

  protected def empty(start: Instant, candleLength: Duration): CandleHistorySegment =
    CandleHistorySegment.empty(start, candleLength)

  protected def fromCandles(cc: Candle*): CandleHistorySegment = CandleHistorySegment.fromCandles(cc)

}
