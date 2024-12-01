package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class TradeHistorySegmentTest_Truncate extends TradeHistorySegmentTest {

  test("truncating an empty segment does not change it") {
    val seg = tradeHistorySegment(sec(100))()
    seg.truncate(sec(99)) shouldEqual seg
    seg.truncate(sec(100)) shouldEqual seg
    seg.truncate(sec(101)) shouldEqual seg
  }

  test("truncating a segment removes all candles ending at or after the given time") {
    val seg = tradeHistorySegment(sec(100))(
      trade(sec(101), "A"),
      trade(sec(102), "B"),
      trade(sec(103), "C"),
      trade(sec(104), "D"),
    )
    seg.truncate(sec(103)) shouldEqual tradeHistorySegment(sec(100))(
      trade(sec(101), "A"),
      trade(sec(102), "B"),
    )
  }

}