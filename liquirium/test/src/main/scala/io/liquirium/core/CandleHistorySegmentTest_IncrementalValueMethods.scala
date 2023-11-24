package io.liquirium.core

import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}

class CandleHistorySegmentTest_IncrementalValueMethods extends CandleHistorySegmentTest {

  test("the empty segment has no 'prev' and 'lastIncrement'") {
    empty(sec(10), secs(5)).prev shouldEqual None
    empty(sec(10), secs(5)).lastIncrement shouldEqual None
  }

  test("'inc' is an alias for 'append'") {
    val s1 = empty(sec(10), secs(5))
    val s2 = s1.append(c5(sec(10), 1))
    val s3 = s2.append(c5(sec(15), 1))
    s2 shouldEqual candleHistorySegment(
      c5(sec(10), 1),
    )
    s3 shouldEqual candleHistorySegment(
      c5(sec(10), 1),
      c5(sec(15), 1),
    )
  }

  test("'prev' yields the predecessor of a non-empty segment") {
    val s1 = empty(sec(10), secs(5))
    val s2 = s1.append(c5(sec(10), 1))
    val s3 = s2.append(c5(sec(15), 1))
    s2.prev.get should be theSameInstanceAs s1
    s3.prev.get should be theSameInstanceAs s2
  }

  test("'lastIncrement' yields the latest appended candle") {
    val s1 = empty(sec(10), secs(5))
    val s2 = s1.append(c5(sec(10), 1))
    val s3 = s2.append(c5(sec(15), 1))
    s2.lastIncrement.get shouldEqual c5(sec(10), 1)
    s3.lastIncrement.get shouldEqual c5(sec(15), 1)
  }

}
