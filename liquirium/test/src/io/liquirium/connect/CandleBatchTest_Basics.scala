package io.liquirium.connect

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.{c10, c5, candleBatch}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import org.scalatest.matchers.should.Matchers.{an, thrownBy}

class CandleBatchTest_Basics extends BasicTest {

  test("it may contain gaps") {
    candleBatch(start = sec(10), candleLength = secs(5))(
      c5(sec(15), 1),
      c5(sec(25), 1),
    )
  }

  test("it throws an exception when candles are not properly ordered") {
    an[Exception] shouldBe thrownBy {
      candleBatch(start = sec(10), candleLength = secs(5))(
        c5(sec(15), 1),
        c5(sec(25), 1),
        c5(sec(20), 1),
      )
    }
  }

  test("it throws an exception when it includes candles before the start") {
    an[Exception] shouldBe thrownBy {
      candleBatch(start = sec(20), candleLength = secs(5))(
        c5(sec(15), 1),
        c5(sec(20), 1),
      )
    }
  }

  test("it throws an exception when it includes candles after the next batch start") {
    an[Exception] shouldBe thrownBy {
      candleBatch(start = sec(10), candleLength = secs(5), nextBatchStart = Some(sec(25)))(
        c5(sec(10), 1),
        c5(sec(25), 1),
      )
    }
  }

  test("it throws an exception when it includes candles with a different length") {
    an[Exception] shouldBe thrownBy {
      candleBatch(start = sec(10), candleLength = secs(5))(
        c5(sec(10), 1),
        c10(sec(15), 1),
      )
    }
  }

  test("it throws an exception when a candle is not properly aligned") {
    an[Exception] shouldBe thrownBy {
      candleBatch(start = sec(10), candleLength = secs(5))(
        c5(sec(10), 1),
        c5(sec(16), 1),
      )
    }
  }

  test("it throws an exception when the next batch start is not properly aligned") {
    an[Exception] shouldBe thrownBy {
      candleBatch(start = sec(10), candleLength = secs(5), nextBatchStart = Some(sec(16)))(
        c5(sec(10), 1),
      )
    }
  }

  test("it throws an exception when the next batch start is earlier than the start") {
    an[Exception] shouldBe thrownBy {
      candleBatch(start = sec(10), candleLength = secs(5), nextBatchStart = Some(sec(5)))()
    }
  }

  test("it throws an exception when the next batch start is equal to the start") {
    an[Exception] shouldBe thrownBy {
      candleBatch(start = sec(10), candleLength = secs(5), nextBatchStart = Some(sec(10)))()
    }
  }

}
