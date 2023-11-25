package io.liquirium.connect.coinbase

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.secs

class CoinbaseCandleLengthTest extends BasicTest {

  test("a supported length can be obtained for the given duration") {
    CoinbaseCandleLength.forDuration(secs(3600)) shouldEqual CoinbaseCandleLength.oneHour
  }

  test("an exception is thrown when the given length is not supported") {
    an[Exception] shouldBe thrownBy(CoinbaseCandleLength.forDuration(secs(123)))
  }

  test("a supported length can be obtained for the given code") {
    CoinbaseCandleLength.forCode("FIVE_MINUTE") shouldEqual CoinbaseCandleLength.fiveMinutes
  }

  test("an exception is thrown when the given code is not supported") {
    an[Exception] shouldBe thrownBy(CoinbaseCandleLength.forCode("THREE_MINUTE"))
  }

}
