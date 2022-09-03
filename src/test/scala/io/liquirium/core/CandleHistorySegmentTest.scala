package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.{dec, sec, secs}
import io.liquirium.core.helper.{BasicTest, CandleHelpers}

import java.time.{Duration, Instant}

class CandleHistorySegmentTest extends BasicTest {

  protected def c5(start: Instant, n: Int): Candle = CandleHelpers.ohlcCandle(
    start = start,
    length = secs(5),
    ohlc = CandleHelpers.ohlc(n),
    quoteVolume = dec(1),
  )

  protected def c10(start: Instant, n: Int): Candle = CandleHelpers.ohlcCandle(
    start = start,
    length = secs(10),
    ohlc = CandleHelpers.ohlc(n),
    quoteVolume = dec(1),
  )

  protected def e5(start: Instant): Candle = CandleHelpers.emptyCandle(start, length = secs(5))

  protected def fromForwardCandles(start: Instant, resolution: Duration)(cc: Candle*): CandleHistorySegment =
    CandleHistorySegment.fromForwardCandles(start, resolution, cc)

  protected def segment(c0: Candle, cc: Candle*): CandleHistorySegment =
    fromForwardCandles(c0.startTime, c0.length)(c0 :: cc.toList: _*)

  protected def empty(start: Instant, resolution: Duration): CandleHistorySegment =
    CandleHistorySegment.empty(start, resolution)

}
