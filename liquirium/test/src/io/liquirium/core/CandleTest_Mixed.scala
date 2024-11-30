package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.candle
import io.liquirium.core.helpers.CoreHelpers._
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}

class CandleTest_Mixed extends BasicTest {

  test("the price can be mapped") {
    candle(price = 1.0, volume = 1.0).mapPrice(_ * 2) shouldEqual candle(price = 2.0, volume = 1.0)
  }

  test("high and low are adjusted if necessary when mapping the price") {
    candle(open = 1.0, close = 1.0, high = 2.0, low = 0.25).mapPrice(1 / _) should equal(
      candle(open = 1.0, close = 1.0, high = 4, low = 0.5))
  }

  test("the prices can be scaled") {
    candle(open = 1.0, close = 1.0, high = 2.0, low = 0.25).scale(1.5) should equal(
      candle(open = 1.5, close = 1.5, high = 3, low = 0.375))
  }

  test("the end time is the start time plus the duration") {
    candle(start = sec(123), length = secs(10)).endTime shouldEqual sec(133)
  }

  test("the history id is the start time in millis as string") {
    candle(start = sec(123)).historyId shouldEqual "123000"
  }

  test("the history timestamp is the start time") {
    candle(start = sec(123)).historyTimestamp shouldEqual sec(123)
  }

  test("an empty candle has start and length but volume and prices are set to zero") {
    Candle.empty(start = sec(123), length = secs(10)) shouldEqual Candle(
      startTime = sec(123),
      length = secs(10),
      open = BigDecimal(0),
      high = BigDecimal(0),
      low = BigDecimal(0),
      close = BigDecimal(0),
      quoteVolume = BigDecimal(0)
    )
  }

  test("a candle is considered empty when the quote volume is empty") {
    candle(open = 1.0, close = 1.0, high = 2.0, low = 0.25, quoteVolume = BigDecimal(0)).isEmpty shouldBe true
    candle(open = 1.0, close = 1.0, high = 2.0, low = 0.25, quoteVolume = BigDecimal(1)).isEmpty shouldBe false
  }

}
