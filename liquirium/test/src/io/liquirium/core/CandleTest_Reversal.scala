package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.candle
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CandleTest_Reversal extends BasicTest {

  test("the start time becomes the end time") {
    candle(start = sec(200), length = secs(100)).reverse.startTime shouldEqual sec(300)
  }

  test("the open price becomes the close price and vice versa") {
    candle(close = 2.1).reverse.open shouldEqual 2.1
    candle(open = 1.2).reverse.close shouldEqual 1.2
  }

  test("a sequence of candles can be reversed") {
    val (c1, c2) = (candle(open = 1.0, close = 2.0), candle(open = 3.0, close = 4.0))
    Candle.reverse(Seq(c1, c2)) shouldEqual Seq(c2.reverse, c1.reverse)
  }

}
