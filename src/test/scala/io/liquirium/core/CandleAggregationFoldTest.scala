package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment, e5}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}

import java.time.Duration

class CandleAggregationFoldTest extends BasicTest {

  private def aggregate(cc: Candle*) = Candle.aggregate(cc)

  def fold(newCandleLength: Duration, chs: CandleHistorySegment): CandleHistorySegment =
    CandleAggregationFold(newCandleLength).fold(chs).value.completedAggregates

  test("applying the fold to an empty segment yields an empty segment with the same start but longer candle length") {
    val chs = candleHistorySegment(sec(100), secs(5))()
    fold(secs(10), chs) shouldEqual candleHistorySegment(sec(100), secs(10))()
  }

  test("the new segment start is moved back to align with the new candle length") {
    val chs = candleHistorySegment(sec(100), secs(5))()
    fold(secs(30), chs) shouldEqual candleHistorySegment(sec(90), secs(30))()
  }

  test("it throws an exception when new candle length is not divisible by base length") {
    val chs = candleHistorySegment(sec(100), secs(5))()
    an[Exception] shouldBe thrownBy (fold(secs(11), chs))
  }

  test("only when a batch is filled are the candles aggregated and added to the aggregate batch") {
    val chs0 = candleHistorySegment(c5(sec(10), 1))
    fold(secs(10), chs0) shouldEqual candleHistorySegment(sec(10), secs(10))()
    val chs1 = chs0.inc(c5(sec(15), 2))
    fold(secs(10), chs1) shouldEqual candleHistorySegment(
      aggregate(c5(sec(10), 1), c5(sec(15), 2))
    )
  }

  test("aggregate candle alignment is fixed so the first aggregate batch may have to be padded") {
    val chs = candleHistorySegment(
      c5(sec(15), 1),
      c5(sec(20), 2),
      c5(sec(25), 3),
    )
    val expected = candleHistorySegment(
      aggregate(e5(sec(10)), c5(sec(15), 1)),
      aggregate(c5(sec(20), 2), c5(sec(25), 3)),
    )
    fold(secs(10), chs) shouldEqual expected
  }

}
