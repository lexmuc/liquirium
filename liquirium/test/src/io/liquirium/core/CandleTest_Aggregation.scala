package io.liquirium.core

import io.liquirium.core.helpers.CandleHelpers.{candle, emptyCandle}
import io.liquirium.core.helpers.CoreHelpers._
import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CandleTest_Aggregation extends BasicTest {

  private def aggregate(cc: Candle*) = Candle.aggregate(cc)

  test("the aggregated candle has the earliest start") {
    aggregate(
      candle(start = sec(15)),
      candle(start = sec(10)),
      candle(start = sec(20))
    ).startTime shouldEqual sec(10)
  }

  test("the aggregated candle has the latest end (via length)") {
    aggregate(
      candle(start = sec(10), length = secs(5)),
      candle(start = sec(20), length = secs(10)),
      candle(start = sec(15), length = secs(5))
    ).endTime shouldEqual sec(30)
  }

  test("the open price is the open price of the earliest candle") {
    aggregate(
      candle(start = sec(15), open = dec(1)),
      candle(start = sec(10), open = dec(2)),
      candle(start = sec(20), open = dec(3))
    ).open shouldEqual dec(2)
  }

  test("the close price is the close price of the latest candle") {
    aggregate(
      candle(start = sec(10), length = secs(5), close = dec(1)),
      candle(start = sec(20), length = secs(5), close = dec(2)),
      candle(start = sec(15), length = secs(5), close = dec(3))
    ).close shouldEqual dec(2)
  }

  test("the high price is the max price of all candles") {
    aggregate(
      candle(start = sec(15), high = dec(1)),
      candle(start = sec(10), high = dec(3)),
      candle(start = sec(20), high = dec(2))
    ).high shouldEqual dec(3)
  }

  test("the low price is the min price of all candles") {
    aggregate(
      candle(start = sec(15), low = dec(3)),
      candle(start = sec(10), low = dec(1)),
      candle(start = sec(20), low = dec(2))
    ).low shouldEqual dec(1)
  }

  test("empty candles are ignored for all prices") {
    val a = aggregate(
      emptyCandle(start = sec(5), length = secs(5)),
      candle(start = sec(10), length = secs(5), open = dec(2), high = dec(4), low = dec(1), close=dec(3)),
      emptyCandle(start = sec(20), length = secs(5))
    )
    a.open shouldEqual dec(2)
    a.high shouldEqual dec(4)
    a.low shouldEqual dec(1)
    a.close shouldEqual dec(3)
  }

  test("if all candles are empty the result candle is empty as well") {
    aggregate(
      emptyCandle(start = sec(5), length = secs(5)),
      emptyCandle(start = sec(20), length = secs(5))
    ) shouldEqual emptyCandle(start = sec(5), length = secs(20))
  }

  test("the quote volume is the total quote volume of all candles") {
    aggregate(
      candle(start = sec(15), quoteVolume = dec(3)),
      candle(start = sec(10), quoteVolume = dec(1)),
      candle(start = sec(20), quoteVolume = dec(2))
    ).quoteVolume shouldEqual dec(6)
  }

}
