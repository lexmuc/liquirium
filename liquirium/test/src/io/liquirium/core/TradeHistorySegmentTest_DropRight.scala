package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TradeHelpers.trade

class TradeHistorySegmentTest_DropRight extends TradeHistorySegmentTest {

  test("dropping elements from the right yields exactly the predecessor segment objects") {
    val s0 = empty(sec(0))
    val s1 = s0.append(trade(sec(1), "1"))
    val s2 = s1.append(trade(sec(2), "2"))
    s2.dropRight(1) should be theSameInstanceAs s1
    s2.dropRight(2) should be theSameInstanceAs s0
    s1.dropRight(1) should be theSameInstanceAs s0
  }

  test("dropping more elements from the right than the segment contains yields the empty segment") {
    val s0 = empty(sec(0))
    val s1 = s0.append(trade(sec(1), "1"))
    s1.dropRight(2) should be theSameInstanceAs s0
  }

}
