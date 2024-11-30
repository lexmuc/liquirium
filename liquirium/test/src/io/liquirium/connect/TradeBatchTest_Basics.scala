package io.liquirium.connect

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeBatch}
import org.scalatest.matchers.should.Matchers.{an, thrownBy}

class TradeBatchTest_Basics extends BasicTest {

  test("it may be empty") {
    tradeBatch(sec(10))()
  }

  test("it throws an exception when trades are not ordered by time") {
    an[Exception] shouldBe thrownBy {
      tradeBatch(sec(10))(
        trade(sec(15), "A"),
        trade(sec(20), "B"),
        trade(sec(17), "C"),
      )
    }
  }

  test("it throws an exception when trades with the same time are not ordered by id") {
    an[Exception] shouldBe thrownBy {
      tradeBatch(sec(10))(
        trade(sec(15), "A"),
        trade(sec(15), "C"),
        trade(sec(15), "B"),
      )
    }
  }

  test("it throws an exception when it includes trades before the start") {
    an[Exception] shouldBe thrownBy {
      tradeBatch(sec(10))(
        trade(sec(9), "A"),
        trade(sec(11), "B"),
      )
    }
  }

  test("it throws an exception when the next batch start is earlier than the start") {
    an[Exception] shouldBe thrownBy {
      tradeBatch(start = sec(10), nextBatchStart = Some(sec(5)))()
    }
  }

  test("it throws an exception when the next batch start is equal to the start") {
    an[Exception] shouldBe thrownBy {
      tradeBatch(start = sec(10), nextBatchStart = Some(sec(10)))()
    }
  }

}
