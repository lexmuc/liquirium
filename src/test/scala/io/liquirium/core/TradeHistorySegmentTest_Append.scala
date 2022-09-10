package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.sec
import io.liquirium.core.helper.TradeHelpers.{trade, tradeHistorySegment => segment}

class TradeHistorySegmentTest_Append extends TradeHistorySegmentTest {

  test("a trade in chronological order (on end) can be appended") {
    val ths = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b")
    )
    val t = trade(sec(2), "c")

    ths.append(t) shouldEqual segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(2), "c")
    )
  }
  
  test("a trade in chronological order (later than end) can be appended") {
    val ths = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b")
    )
    val t = trade(sec(3), "c")

    ths.append(t) shouldEqual segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b"),
      trade(sec(3), "c")
    )
  }
  
  test("an exception is thrown when trying to append a trade that is not in chronological order") {
    val ths = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b")
    )
    val t = trade(sec(1), "c")

    an[Exception] shouldBe thrownBy {
      ths.append(t)
    }
  }

  test("an exception is thrown when trying to append a trade whose id already exists") {
    val ths = segment(
      sec(0),
      trade(sec(1), "a"),
      trade(sec(2), "b")
    )
    val t = trade(sec(3), "b")

    an[Exception] shouldBe thrownBy {
      ths.append(t)
    }
  }
}


