package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.secs

class PoloniexCandleLengthTest extends BasicTest {

  test("a supported length can be obtained for the given duration") {
    PoloniexCandleLength.forDuration(secs(300)) shouldEqual PoloniexCandleLength.fiveMinutes
  }

  test("an exception is thrown when the given length is not supported") {
    an[Exception] shouldBe thrownBy(PoloniexCandleLength.forDuration(secs(123)))
  }

  test("a supported length can be obtained for the given code") {
    PoloniexCandleLength.forCode("MINUTE_1") shouldEqual PoloniexCandleLength.oneMinute
  }

  test("an exception is thrown when the given code is not supported") {
    an[Exception] shouldBe thrownBy(PoloniexCandleLength.forCode("MINUTE_7"))
  }

}
