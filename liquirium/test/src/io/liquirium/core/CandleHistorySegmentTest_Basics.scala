package io.liquirium.core

import io.liquirium.core.helpers.CandleHelpers.{c10, c5, candleHistorySegment => segment}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}

import scala.annotation.tailrec

class CandleHistorySegmentTest_Basics extends CandleHistorySegmentTest {

  test("it can be created empty only with start and candleLength") {
    val chs = empty(sec(10), secs(5))
    chs.start shouldEqual sec(10)
    chs.candleLength shouldEqual secs(5)
    chs shouldEqual List()
  }

  test("the end of an empty segment is equal to the start") {
    empty(start = sec(10), candleLength = secs(5)).end shouldEqual sec(10)
  }

  test("it can be treated as a sequence") {
    val chs = segment(
      c5(sec(10), 1),
      c5(sec(15), 1),
    )
    chs.length shouldEqual 2
    chs.drop(1) shouldEqual segment(c5(sec(15), 1))
    chs.take(1) shouldEqual segment(c5(sec(10), 1))
    chs.head shouldEqual c5(sec(10), 1)
    chs(1) shouldEqual c5(sec(15), 1)
  }

  test("a single candle can be appended") {
    segment(c5(sec(10), 1)).append(c5(sec(15), 2)) shouldEqual segment(c5(sec(10), 1), c5(sec(15), 2))
    empty(sec(10), secs(5)).append(c5(sec(10), 2)) shouldEqual segment(c5(sec(10), 2))
  }

  test("an exception is thrown when an appended candle does not start at the end of the segment") {
    an[Exception] shouldBe thrownBy {
      segment(c5(sec(10), 1)).append(c5(sec(20), 2))
    }
    an[Exception] shouldBe thrownBy {
      empty(sec(10), secs(5)).append(c5(sec(15), 2))
    }
  }

  test("an exception is thrown when an appended candle has a different length") {
    an[Exception] shouldBe thrownBy {
      segment(c5(sec(10), 1)).append(c10(sec(15), 2))
    }
    an[Exception] shouldBe thrownBy {
      empty(sec(10), secs(5)).append(c10(sec(10), 2))
    }
  }

  test("dropping elements from the right yields exactly the predecessor segment objects") {
    val s0 = empty(sec(10), secs(5))
    val s1 = s0.append(c5(sec(10), 1))
    val s2 = s1.append(c5(sec(15), 1))
    s2.dropRight(1) should be theSameInstanceAs s1
    s2.dropRight(2) should be theSameInstanceAs s0
    s1.dropRight(1) should be theSameInstanceAs s0
  }

  test("dropping more elements from the right than the segment contains yields the empty segment") {
    val s0 = empty(sec(10), secs(5))
    val s1 = s0.append(c5(sec(10), 1))
    s1.dropRight(2) should be theSameInstanceAs s0
  }

  test("init yields exactly the predecessor element") {
    val s0 = empty(sec(10), secs(5))
    val s1 = s0.append(c5(sec(10), 1))
    val s2 = s1.append(c5(sec(15), 1))
    s2.init should be theSameInstanceAs s1
    s1.init should be theSameInstanceAs s0
  }

  test("calling init on the empty segment yields and UnsupportedOperationException") {
    an[UnsupportedOperationException] shouldBe thrownBy(empty(sec(10), secs(5)).init)
  }

  test("lastOption works as expected") {
    val s0 = empty(sec(10), secs(5))
    val s1 = s0.append(c5(sec(10), 1))
    s0.lastOption shouldEqual None
    s1.lastOption shouldEqual Some(c5(sec(10), 1))
  }

  test("an exception is thrown when accessing an index out of bound") {
    val s0 = empty(sec(10), secs(5))
    an[IndexOutOfBoundsException] shouldBe thrownBy(s0(1))
  }

  test("the reverse iterator yields the elements in reverse order") {
    val s0 = empty(sec(10), secs(5))
    val s1 = s0.append(c5(sec(10), 1))
    val s2 = s1.append(c5(sec(15), 1))
    s2.reverseIterator.toList shouldEqual List(
      c5(sec(15), 1),
      c5(sec(10), 1),
    )
  }

  test("creating and using long segments is stack-safe") {
    @tailrec
    def go(chs: CandleHistorySegment, n: Int): CandleHistorySegment =
      if (n > 0) go(chs.append(c5(chs.end, 1)), n - 1)
      else chs
    val x = go(empty(sec(0), secs(5)), 10000)
    x(1)
    x.iterator
  }

}
