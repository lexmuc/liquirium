package io.liquirium.connect.binance

import io.liquirium.core.helper.BasicTest
import io.liquirium.core.helper.CoreHelpers.secs

class BinanceCandleResolutionTest extends BasicTest {

  test("a supported resolution can be obtained for the given duration") {
    BinanceCandleResolution.forDuration(secs(3600)) shouldEqual BinanceCandleResolution.oneHour
  }

  test("an exception is thrown when the given resolution (duration) is not supported") {
    an[Exception] shouldBe thrownBy(BinanceCandleResolution.forDuration(secs(123)))
  }

}
