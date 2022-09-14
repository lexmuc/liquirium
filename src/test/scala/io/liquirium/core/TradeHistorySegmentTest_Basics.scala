package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.sec
import io.liquirium.core.helper.TradeHelpers.{trade, tradeHistorySegment => segment}

class TradeHistorySegmentTest_Basics extends TradeHistorySegmentTest {

  test("it can be created empty only with start") {
    val ths = empty(sec(5))
    ths.start shouldEqual sec(5)
    ths shouldEqual List()
  }

  test("the end of an empty segment is equal to the start") {
    empty(sec(5)).end shouldEqual sec(5)
  }

  test("it can be treated as a sequence") {
    val ths = segment(
      sec(0),
      trade(sec(1), "1"),
      trade(sec(2), "2"),
    )
    ths.length shouldEqual 2
    ths.take(1) shouldEqual segment(sec(0), trade(sec(1), "1"))
    ths.drop(1) shouldEqual segment(sec(0), trade(sec(2), "2"))
    ths.head shouldEqual trade(sec(1), "1")
    ths(1) shouldEqual trade(sec(2), "2")
  }

  test("init yields exactly the predecessor element") {
    val s0 = empty(sec(0))
    val s1 = s0.append(trade(sec(1), "1"))
    val s2 = s1.append(trade(sec(2), "2"))
    s2.init should be theSameInstanceAs s1
    s1.init should be theSameInstanceAs s0
  }

  test("calling init on the empty segment yields and UnsopportedOperationException") {
    an[UnsupportedOperationException] shouldBe thrownBy(empty(sec(0)).init)
  }

  test("lastOption works as expected") {
    val s0 = empty(sec(0))
    val s1 = s0.append(trade(sec(1), "1"))
    s0.lastOption shouldEqual None
    s1.lastOption shouldEqual Some(trade(sec(1), "1"))
  }

  test("an exception is thrown when accessing an index out of bound") {
    val s0 = empty(sec(0))
    an[IndexOutOfBoundsException] shouldBe thrownBy(s0(1))
  }
}
