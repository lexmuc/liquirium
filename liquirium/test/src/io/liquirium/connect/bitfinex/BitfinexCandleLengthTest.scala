package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.secs
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}

class BitfinexCandleLengthTest extends BasicTest {

  test("a supported candle length can be obtained for the given duration") {
    BitfinexCandleLength.forDuration(secs(3600)) shouldEqual BitfinexCandleLength.oneHour
  }

  test("an exception is thrown when the given candle length is not supported") {
    an[Exception] shouldBe thrownBy(BitfinexCandleLength.forDuration(secs(123)))
  }

  test("it exposes the candle length as duration") {
    BitfinexCandleLength("code", 123).candleLength shouldEqual secs(123)
  }

}
