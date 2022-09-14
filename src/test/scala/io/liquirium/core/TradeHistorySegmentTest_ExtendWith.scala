package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.{milli, sec}
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

  test("when extending with a segment starting earlier than the own ends overlapping candles are overwritten") {
    val s1 = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(3), "c"),
    )
    val s2 = segment(
      sec(2),
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )

    s1.extendWith(s2) shouldEqual segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )
  }

  test("extending a segment with another one starting earlier overwrites it up to the start") {

    val s1 = segment(
      sec(5),
      trade(sec(5), "a"),
      trade(sec(6), "b"),
    )
    val s2 = segment(
      sec(2),
      trade(sec(2), "c"),
    )

//        s1.extendWith(s2)
    // Hier 'Trades must be in chronological order' Error
    // Das sollte aber möglich sein? Mit welchem Ergebnis? Empty segment mit start von s1?

    //    val e = empty(sec(5), secs(5))
    //    s1.extendWith(e) shouldEqual empty(sec(10), secs(5))
  }


  test("an exception is thrown when trying to extend with a segment starting later than the first ends") {
    val s1 = segment(sec(0), trade(milli(1), "1"))
    val s2 = segment(milli(2), trade(sec(2), "2"))
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

  test("the internal list is not changed more than necessary (unchanged part remains the same)") {
    val s1 = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(3), "c"),
    )
    val s2 = segment(
      sec(3),
      trade(sec(3), "d"),
      trade(sec(4), "e"),
    )

    s1.extendWith(s2).dropRight(2) should be theSameInstanceAs s1.dropRight(1)
  }

}