package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment => segment}
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}

class TradeHistorySegmentTest_Append extends TradeHistorySegmentTest {

  test("an exception is thrown when trying to append a trade earlier than the segment start") {
    val ths = TradeHistorySegment.empty(sec(10))
    an[Exception] shouldBe thrownBy(ths.append(trade(sec(9), "a")))
  }

  test("it is possible to append a trade with the same time as the last trade when the id is different") {
    val ths = segment(sec(0))(
      trade(sec(1), "x"),
    )
    ths.append(trade(sec(1), "a")) shouldEqual segment(sec(0))(
      trade(sec(1), "x"),
      trade(sec(1), "a")
    )
  }
  
  test("it is possible to append a trade with a later time regardless of the id") {
    val ths = segment(sec(0))(
      trade(sec(1), "b"),
    )

    ths.append(trade(sec(2), "a")) shouldEqual segment(sec(0))(
      trade(sec(1), "b"),
      trade(sec(2), "a"),
    )
  }
  
  test("an exception is thrown when trying to append a trade that is not in chronological order") {
    val ths = segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
    )
    an[Exception] shouldBe thrownBy {
      ths.append(trade(sec(1), "c"))
    }
  }

  test("an exception is thrown when trying to append a trade with an already known id") {
    val ths = segment(sec(0))(
      trade(sec(1), "a"),
      trade(sec(2), "b"),
    )
    an[Exception] shouldBe thrownBy {
      ths.append(trade(sec(3), "b"))
    }
  }

}