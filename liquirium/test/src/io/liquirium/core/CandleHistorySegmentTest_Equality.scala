package io.liquirium.core

import io.liquirium.core.helpers.CandleHelpers.candleHistorySegment
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}

class CandleHistorySegmentTest_Equality extends CandleHistorySegmentTest {

  test("equality checks with other non-empty segments work properly") {
    val chs = candleHistorySegment(sec(10), secs(5))(1, 2, 3)
    chs shouldEqual chs
    chs shouldEqual candleHistorySegment(sec(10), secs(5))(1, 2, 3)
    chs should not equal candleHistorySegment(sec(10), secs(5))(1, 2)
  }

  test("another empty segment is only equal if the start and the candleLength is equal") {
    val chs = candleHistorySegment(sec(10), secs(5))()
    chs should not equal candleHistorySegment(sec(10), secs(10))()
    chs should not equal candleHistorySegment(sec(15), secs(5))()
    chs shouldEqual candleHistorySegment(sec(10), secs(5))()
  }

  test("equality checks with other sequences work properly") {
    val chs = candleHistorySegment(sec(10), secs(5))(1, 2, 3)
    chs shouldEqual List(chs: _*)
    chs should not equal List(chs: _*).take(2)
  }

  test("equality checks with different values all yield false") {
    val chs = candleHistorySegment(sec(10), secs(5))(1, 2, 3)
    chs should not equal Set(1, 2, 3)
    chs should not equal 1
  }

}
