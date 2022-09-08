package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.sec
import io.liquirium.core.helper.TradeHelpers.{trade, tradeHistorySegment => segment}

class TradeHistorySegmentTest_ExtendWith extends TradeHistorySegmentTest {

    test("one segment can be extended with another one starting at or before the last trade") {
      val s1 = segment(
        sec(0),
        trade(1, sec(1)),
        trade(2, sec(2)),
      )
      val s2 = segment(
        sec(2),
        trade(2, sec(2)),
        trade(3, sec(3)),
        trade(4, sec(4)),
      )
      s1.extendWith(s2) shouldEqual segment(
        sec(0),
        trade(1, sec(1)),
        trade(2, sec(2)),
        trade(3, sec(3)),
        trade(4, sec(4)),
      )

      val s3 = segment(
        sec(0),
        trade(1, sec(1)),
        trade(3, sec(3))
      )
      val s4 = segment(
        sec(2),
        trade(2, sec(2)),
        trade(3, sec(3)),
        trade(4, sec(4))
      )
      s3.extendWith(s4) shouldEqual segment(
        sec(0),
        trade(1, sec(1)),
        trade(2, sec(2)),
        trade(3, sec(3)),
        trade(4, sec(4))
      )
    }

    test("an exception is thrown when trying to extend with a segment starting later than the first ends") {
      val s1 = segment(sec(0), trade(1, sec(0)))
      val s2 = segment(sec(2), trade(2, sec(2)))
      an[Exception] shouldBe thrownBy {
        s1.extendWith(s2)
      }
      an[Exception] shouldBe thrownBy {
        s1.extendWith(empty(sec(20)))
      }
    }

}