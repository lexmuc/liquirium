package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment => segment}

class TradeHistorySegmentTest_IncrementalValueMethods extends TradeHistorySegmentTest {

  test("incrementing a segment means appending a trade") {
    val ths = segment(sec(0))()
    val ths1 = segment(sec(0))().inc(trade(sec(1), "A"))
    val ths2 = ths1.inc(trade(sec(2), "B"))
    ths1 shouldEqual ths.append(trade(sec(1), "A"))
    ths2 shouldEqual ths1.append(trade(sec(2), "B"))
  }

  test("the empty segment has no last increment or previous value") {
    val ths = segment(sec(0))()
    ths.prev shouldBe None
    ths.lastIncrement shouldBe None
  }

  test("non-empty segments have last increment and previous value properly set") {
    val ths1 = segment(sec(0))(trade(sec(1), "A"))
    val ths2 = ths1.inc(trade(sec(2), "B"))
    ths2.prev.get should be theSameInstanceAs ths1
    ths2.lastIncrement shouldEqual Some(trade(sec(2), "B"))
  }

}