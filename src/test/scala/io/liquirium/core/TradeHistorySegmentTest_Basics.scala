package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.sec

class TradeHistorySegmentTest_Basics extends TradeHistorySegmentTest {
  test("it can be created empty only with start") {
    val ths = empty(sec(5))
    ths.start shouldEqual sec(5)
    ths.reverseTrades shouldEqual List()
  }

  test("the end of an empty segment is equal to the start") {
    empty(sec(5)).end shouldEqual sec(5)
  }

}
