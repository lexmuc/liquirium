package io.liquirium.core

import io.liquirium.core.helper.CandleHelpers
import io.liquirium.core.helper.CandleHelpers.{c10, c5}
import io.liquirium.core.helper.CoreHelpers.{sec, secs}
import CandleHelpers.{candleHistorySegment => segment}

class CandleHistorySegmentTest_Append extends CandleHistorySegmentTest {

  test("a single candle can be appended") {
    segment(c5(sec(10), 1)).append(c5(sec(15), 2)) shouldEqual segment(c5(sec(10), 1), c5(sec(15), 2))
    empty(sec(10), secs(5)).append(c5(sec(10), 2)) shouldEqual segment(c5(sec(10), 2))
  }

  test("an exception is thrown when the appended candle does not start at the end of the segment") {
    an[Exception] shouldBe thrownBy {
      segment(c5(sec(10), 1)).append(c5(sec(20), 2))
    }
    an[Exception] shouldBe thrownBy {
      empty(sec(10), secs(5)).append(c5(sec(15), 2))
    }
  }

  test("an exception is thrown when the appended candle has a different resolution") {
    an[Exception] shouldBe thrownBy {
      segment(c5(sec(10), 1)).append(c10(sec(15), 2))
    }
    an[Exception] shouldBe thrownBy {
      empty(sec(10), secs(5)).append(c10(sec(10), 2))
    }
  }

}
