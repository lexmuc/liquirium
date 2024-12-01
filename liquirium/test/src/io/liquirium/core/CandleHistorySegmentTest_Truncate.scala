package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.TradeHelpers.tradeHistorySegment
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CandleHistorySegmentTest_Truncate extends BasicTest {

  test("truncating an empty segment does not change it") {
    val seg = candleHistorySegment(sec(100), secs(10))()
    seg.truncate(sec(99)) shouldEqual seg
    seg.truncate(sec(100)) shouldEqual seg
    seg.truncate(sec(101)) shouldEqual seg
  }

  test("truncating a segment removes all candles ending after the given time") {
    val seg = candleHistorySegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
      c5(sec(110), 1),
      c5(sec(115), 1),
    )
    seg.truncate(sec(110)) shouldEqual candleHistorySegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    seg.truncate(sec(109)) shouldEqual candleHistorySegment(
      c5(sec(100), 1),
    )
  }

}
