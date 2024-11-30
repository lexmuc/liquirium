package io.liquirium.connect.binance

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.secs
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}

class BinanceCandleLengthTest extends BasicTest {

  test("a supported length can be obtained for the given duration") {
    BinanceCandleLength.forDuration(secs(3600)) shouldEqual BinanceCandleLength.oneHour
  }

  test("an exception is thrown when the given length is not supported") {
    an[Exception] shouldBe thrownBy(BinanceCandleLength.forDuration(secs(123)))
  }

}
