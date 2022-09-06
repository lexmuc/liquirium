package io.liquirium.core

import io.liquirium.core.helper.CandleHelpers.{c10, c5, candleHistorySegment => segment}
import io.liquirium.core.helper.CoreHelpers.{sec, secs}

class CandleHistorySegmentTest_ExtendWith extends CandleHistorySegmentTest {

  test("one segment can be extended with another one that directly follows it (may be empty)") {
    val s1 = segment(
      c5(sec(10), 1),
      c5(sec(15), 1),
    )
    val s2 = segment(
      c5(sec(20), 2),
      c5(sec(25), 2),
    )
    s1.extendWith(s2) shouldEqual segment(
      c5(sec(10), 1),
      c5(sec(15), 1),
      c5(sec(20), 2),
      c5(sec(25), 2),
    )
  }

  test("a segment may be extended with an adjacent empty segment") {
    val s1 = segment(
      c5(sec(10), 1),
    )
    s1.extendWith(empty(sec(15), secs(5))) shouldEqual s1
  }

  test("an exception is thrown when trying to extend with a segment starting later than the first ends") {
    val s1 = segment(c5(sec(10), 1))
    val s2 = segment(c5(sec(20), 2))
    an[Exception] shouldBe thrownBy {
      s1.extendWith(s2)
    }
    an[Exception] shouldBe thrownBy {
      s1.extendWith(empty(sec(20), secs(5)))
    }
  }

  test("when extending with a segment starting earlier than the own ends overlapping candles are overwritten") {
    val s1 = segment(
      c5(sec(10), 1),
      c5(sec(15), 1),
    )
    val s2 = segment(
      c5(sec(15), 2),
      c5(sec(20), 2),
    )
    s1.extendWith(s2) shouldEqual segment(
      c5(sec(10), 1),
      c5(sec(15), 2),
      c5(sec(20), 2),
    )
  }

  test("segments can even be shortened by extending them") {
    val s1 = segment(
      c5(sec(10), 1),
      c5(sec(15), 1),
      c5(sec(20), 1),
    )
    val s2 = segment(
      c5(sec(15), 2),
    )
    s1.extendWith(s2) shouldEqual segment(
      c5(sec(10), 1),
      c5(sec(15), 2),
    )
    s1.extendWith(empty(sec(15), secs(5))) shouldEqual segment(
      c5(sec(10), 1),
    )
  }

  test("extending a segment with another one starting earlier overwrites it up to the start") {
    val s1 = segment(
      c5(sec(10), 1),
      c5(sec(15), 1),
      c5(sec(20), 1),
    )
    val s2 = segment(
      c5(sec(5), 2),
      c5(sec(10), 2),
    )
    s1.extendWith(s2) shouldEqual segment(
      c5(sec(10), 2),
    )
    val e = empty(sec(5), secs(5))
    s1.extendWith(e) shouldEqual empty(sec(10), secs(5))
  }

  test("the internal list is not changed more than necessary (unchanged part remains the same)") {
    val s1 = segment(
      c5(sec(10), 1),
      c5(sec(15), 1),
      c5(sec(20), 1),
    )
    val s2 = segment(
      c5(sec(15), 1),
      c5(sec(20), 2),
      c5(sec(25), 2),
    )
    (s1.extendWith(s2).reverseCandles.drop(2) eq s1.reverseCandles.tail) shouldBe true
  }

  test("it throws an exception when the starts are not aligned with regard to the resolution") {
    val s1 = segment(c5(sec(10), 1))
    val s2 = segment(c5(sec(9), 2))
    val s3 = segment(c5(sec(1), 2))
    val e = empty(sec(1), secs(5))
    an[Exception] shouldBe thrownBy(s1.extendWith(s2))
    an[Exception] shouldBe thrownBy(s1.extendWith(s3))
    an[Exception] shouldBe thrownBy(s1.extendWith(e))
  }

  test("it throws an exception when the resolutions are not equal") {
    val s1 = segment(c5(sec(10), 1))
    val s2 = segment(c10(sec(0), 2))
    val e = empty(sec(15), secs(10))
    an[Exception] shouldBe thrownBy(s1.extendWith(s2))
    an[Exception] shouldBe thrownBy(s1.extendWith(e))
  }

}
