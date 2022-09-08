package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.sec
import io.liquirium.core.helper.TradeHelpers.trade


class TradeHistorySegmentTest_FromForwardTrades extends TradeHistorySegmentTest {

  test("it can be created from a forward trade iterable and exposes trades in reverse order") {
    val ths = fromForwardTrades(start = sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2"),
    )
    ths.reverseTrades shouldEqual List(
      trade(sec(2), "2"),
      trade(sec(1), "1"),
    )
  }

  test("it exposes the start too when created from a forward trade iterable") {
    val ths = fromForwardTrades(start = sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2"),
    )
    ths.start shouldEqual sec(0)
  }

  test("the end of a segment is equal to the time of the last trade") {
    val ths = fromForwardTrades(start = sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2"),
    )
    ths.end shouldEqual sec(2)
  }

  test("it throws an exception when trades are not in order") {
    an[Exception] shouldBe thrownBy {
      fromForwardTrades(start = sec(10))(
        trade(sec(20), "1"),
        trade(sec(15), "2"),
      )
    }
  }

  test("it throws an exception when trade ids are not unique") {
    an[Exception] shouldBe thrownBy {
      fromForwardTrades(start = sec(10))(
        trade(sec(15), "1"),
        trade(sec(20), "1"),
      )
    }
  }
}