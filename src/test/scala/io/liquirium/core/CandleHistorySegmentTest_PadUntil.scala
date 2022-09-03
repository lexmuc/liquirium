package io.liquirium.core

import io.liquirium.core.helpers.CandleHelpers.{c5, e5, candleHistorySegment => segment}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}

class CandleHistorySegmentTest_PadUntil extends CandleHistorySegmentTest {

  test("it can be padded with empty candles up to a given point in time") {
    segment(
      c5(sec(10), 1),
      c5(sec(15), 1),
    ).padUntil(sec(30)) shouldEqual segment(
      c5(sec(10), 1),
      c5(sec(15), 1),
      e5(sec(20)),
      e5(sec(25)),
    )
  }

  test("the empty segment can be padded as well") {
    empty(sec(10), candleLength = secs(5)).padUntil(sec(20)) shouldEqual segment(
      e5(sec(10)),
      e5(sec(15)),
    )
  }

  test("if the time is not aligned with the segment it is padded until the time is exceeded") {
    segment(
      c5(sec(10), 1),
    ).padUntil(sec(22)) shouldEqual segment(
      c5(sec(10), 1),
      e5(sec(15)),
      e5(sec(20)),
    )
  }

}
