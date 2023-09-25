package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}

class TradeHistorySegmentTest_TakeFrom extends TradeHistorySegmentTest {

  test("it returns the same segment if the start matches") {
    val segment = tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
    )
    segment.takeFrom(sec(10)) should be theSameInstanceAs segment
  }

  test("it throws a runtime exception when the segment start is before the given time") {
    val segment = tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
    )
    a[RuntimeException] shouldBe thrownBy(segment.takeFrom(sec(9)))
  }

  test("it drops trades before the given time if the time is after the start") {
    val segment = tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
      trade(sec(12), "B-1"),
      trade(sec(12), "B-2"),
      trade(sec(13), "C"),
    )
    segment.takeFrom(sec(12)) shouldEqual tradeHistorySegment(sec(12))(
      trade(sec(12), "B-1"),
      trade(sec(12), "B-2"),
      trade(sec(13), "C"),
    )
  }

  test("the new start is set to the requested time regardless of whether there is a trade at this time") {
    val segment = tradeHistorySegment(sec(10))(
      trade(sec(13), "A"),
    )
    segment.takeFrom(sec(12)).start shouldEqual sec(12)
  }

}
