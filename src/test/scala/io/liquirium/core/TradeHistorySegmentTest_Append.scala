package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.sec
import io.liquirium.core.helper.TradeHelpers.{trade, tradeHistorySegment => segment}

class TradeHistorySegmentTest_Append extends TradeHistorySegmentTest {

  test("it is possible to append a trade with the same time as the last trade when the id is higher") {
    val ths = segment(
      sec(0),
      trade(sec(1), "a"),
    )
    ths.append(trade(sec(1), "b")) shouldEqual segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(1), "b")
    )
  }
  
  test("it is possible to append a trade with a later time regardless of the id") {
    val ths = segment(
      sec(0),
      trade(sec(1), "b"),
    )

    ths.append(trade(sec(2), "a")) shouldEqual segment(
      sec(0),
      trade(sec(1), "b"),
      trade(sec(2), "a"),
    )
  }
  
  test("an exception is thrown when trying to append a trade that is not in chronological order") {
    val ths = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b")
    )
    an[Exception] shouldBe thrownBy {
      ths.append(trade(sec(1), "c"))
    }
  }

  test("an exception is thrown when trying to append a trade at the same time as the last but with lower id") {
    val ths = segment(
      sec(0),
      trade(sec(1), "b")
    )
    an[Exception] shouldBe thrownBy {
      ths.append(trade(sec(1), "a"))
    }
  }

  test("an exception is thrown when trying to append a trade with an already known id") {
    val ths = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b")
    )
    an[Exception] shouldBe thrownBy {
      ths.append(trade(sec(3), "b"))
    }
  }

}