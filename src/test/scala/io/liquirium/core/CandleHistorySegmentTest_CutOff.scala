package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}

class CandleHistorySegmentTest_CutOff extends BasicTest {

  test("cutting off an empty segment does not change it") {
    val seg = candleHistorySegment(sec(100), secs(5))()
    seg.cutOff(sec(99)) shouldEqual seg
    seg.cutOff(sec(100)) shouldEqual seg
    seg.cutOff(sec(101)) shouldEqual seg
  }

  test("cutting off a segment removes all candles ending after the given time") {
    val seg = candleHistorySegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
      c5(sec(110), 1),
      c5(sec(115), 1),
    )
    seg.cutOff(sec(110)) shouldEqual candleHistorySegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    seg.cutOff(sec(109)) shouldEqual candleHistorySegment(
      c5(sec(100), 1),
    )
  }

}
