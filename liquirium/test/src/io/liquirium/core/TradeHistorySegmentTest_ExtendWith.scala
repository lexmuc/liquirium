package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.{dec, milli, sec}
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment => segment}
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}

class TradeHistorySegmentTest_ExtendWith extends TradeHistorySegmentTest {

  test("one segment can be extended with another one starting at the time of the last trade") {
    val s1 = segment(sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2"),
    )
    val s2 = segment(sec(2))(
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )

    s1.extendWith(s2) shouldEqual segment(sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )
  }

  test("a segment can be extended with a partially overlapping segment") {
    val s1 = segment(sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2"),
      trade(sec(3), "3"),
    )
    val s2 = segment(sec(2))(
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )
    s1.extendWith(s2) shouldEqual segment(sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2"),
      trade(sec(3), "3"),
      trade(sec(4), "4"),
    )
  }

  test("if new trades appeared in the overlap they are added") {
    val s1 = segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(3), "c"),
    )
    val s2 = segment(sec(2))(
      trade(sec(2), "b"),
      trade(sec(3), "c"),
    )
    s1.extendWith(s2) shouldEqual segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(3), "c"),
    )
  }

  test("if trades disappear in the overlap they are removed") {
    val s1 = segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(3), "c"),
    )
    val s2 = segment(sec(2))(
      trade(sec(3), "c"),
    )
    s1.extendWith(s2) shouldEqual segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(3), "c"),
    )
  }

  test("all old trades at the update segment start time are potentially overwritten") {
    val s1 = segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(2), "c"),
      trade(sec(2), "d"),
    )
    val s2 = segment(sec(2))(
      trade(sec(2), "c"),
      trade(sec(2), "dx"),
      trade(sec(2), "e"),
    )
    s1.extendWith(s2) shouldEqual segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "c"),
      trade(sec(2), "dx"),
      trade(sec(2), "e"),
    )
  }

  test("if trades in the overlap change (time or content) they are updated") {
    val s1 = segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(3), "c").copy(price = dec(1)),
    )
    val s2 = segment(sec(2))(
      trade(sec(3), "b"),
      trade(sec(3), "c").copy(price = dec(2)),
    )
    s1.extendWith(s2) shouldEqual segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(3), "b"),
      trade(sec(3), "c").copy(price = dec(2)),
    )
  }

  test("extending a segment with another one starting earlier overwrites it up to the start") {
    val s1 = segment(sec(5))(
      trade(sec(5), "e"),
      trade(sec(6), "f"),
    )
    val s2 = segment(sec(2))(
      trade(sec(4), "d"),
      trade(sec(6), "f"),
      trade(sec(7), "g"),
    )
    s1.extendWith(s2) shouldEqual segment(sec(5))(
      trade(sec(6), "f"),
      trade(sec(7), "g"),
    )
  }

  test("extending a segment with another one ending earlier will shorten it") {
    val s1 = segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(3), "c"),
      trade(sec(4), "d"),
    )
    val s2 = segment(sec(3))(
      trade(sec(3), "c"),
    )
    s1.extendWith(s2) shouldEqual segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(3), "c"),
    )
  }

  test("an exception is thrown when trying to extend with a segment starting later than the first ends") {
    val s1 = segment(sec(0))(trade(milli(1), "1"))
    val s2 = segment(milli(2))(trade(sec(2), "2"))
    an[Exception] shouldBe thrownBy {
      s1.extendWith(s2)
    }
    an[Exception] shouldBe thrownBy {
      s1.extendWith(empty(sec(20)))
    }
  }

  test("the internal list is not changed more than necessary (unchanged part remains the same)") {
    val s1 = segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(3), "c").copy(price = dec(1)),
    )
    val s2 = segment(sec(2))(
      trade(sec(2), "b"),
      trade(sec(3), "c").copy(price = dec(2)),
      trade(sec(4), "d"),
    )
    s1.extendWith(s2).dropRight(2) should be theSameInstanceAs s1.dropRight(1)
  }

  test("reuse of the first segment can end between trades with the same timestamp") {
    val s1 = segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(2), "c").copy(price = dec(1)),
    )
    val s2 = segment(sec(2))(
      trade(sec(2), "b"),
      trade(sec(2), "c").copy(price = dec(2)),
    )
    s1.extendWith(s2).dropRight(1) should be theSameInstanceAs s1.dropRight(1)
  }

}