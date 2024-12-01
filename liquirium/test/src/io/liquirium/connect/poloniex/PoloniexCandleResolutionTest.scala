package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.secs
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}

class PoloniexCandleResolutionTest extends BasicTest {

  test("a supported resolution can be obtained for the given duration") {
    PoloniexCandleResolution.forDuration(secs(300)) shouldEqual PoloniexCandleResolution.fiveMinutes
  }

  test("an exception is thrown when the given resolution (duration) is not supported") {
    an[Exception] shouldBe thrownBy(PoloniexCandleResolution.forDuration(secs(123)))
  }

  test("it exposes the candle length as duration") {
    PoloniexCandleResolution(123).candleLength shouldEqual secs(123)
  }

}
