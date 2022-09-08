package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.sec
import io.liquirium.core.helper.TradeHelpers.{trade, tradeHistorySegment => segment}

class TradeHistorySegmentTest_ExtendWith extends TradeHistorySegmentTest {

  test("one segment can be extended with another one starting at the end of last trade") {
    val s1 = segment(
      sec(0),
      trade(sec(1), "1"),
      trade(sec(2), "2"),
    )
    val s2 = segment(
      sec(2),
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )

    s1.extendWith(s2) shouldEqual segment(
      sec(0),
      trade(sec(1), "1"),
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )
  }

  test("one segment can be extended with another one starting before end of last trade") {
    val s1 = segment(
      sec(0),
      trade(sec(1), "1"),
      trade(sec(3), "3"),
    )
    val s2 = segment(
      sec(2),
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )
    s1.extendWith(s2) shouldEqual segment(
      sec(0),
      trade(sec(1), "1"),
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )
  }

  test("an exception is thrown when trying to extend with a segment starting later than the first ends") {
    val s1 = segment(sec(0), trade(sec(0), "1"))
    val s2 = segment(sec(2), trade(sec(2), "2"))
    an[Exception] shouldBe thrownBy {
      s1.extendWith(s2)
    }
    an[Exception] shouldBe thrownBy {
      s1.extendWith(empty(sec(20)))
    }
  }

  //  Selber test wie "extended with another one starting before end" ? Wahrscheinlich anders?
  test("trades may disappear through extending") {
    val s1 = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(3), "b"),
    )
    val s2 = segment(
      sec(2),
      trade(sec(2), "c"),
      trade(sec(3), "d"),
      trade(sec(4), "e"),
    )
    s1.extendWith(s2) shouldEqual segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "c"),
      trade(sec(3), "d"),
      trade(sec(4), "e"),
    )
  }

  test("the time of a trade may change") {
    val s1 = segment(
      sec(0),
      trade(sec(3), "a"),
      trade(sec(4), "b"),
    )
    val s2 = segment(
      sec(2),
      trade(sec(2), "a"),
      trade(sec(3), "b"),
    )
    s1.extendWith(s2) shouldEqual segment(
      sec(0),
      trade(sec(2), "a"),
      trade(sec(3), "b")
    )
  }

  test("new trades may be inserted in between old trades") {
    val s1 = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(4), "b"),
    )
    val s2 = segment(
      sec(2),
      trade(sec(2), "c"),
      trade(sec(3), "d"),
      trade(sec(4), "b"),
    )
    s1.extendWith(s2) shouldEqual segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "c"),
      trade(sec(3), "d"),
      trade(sec(4), "b"),
    )
  }

}