package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.{poloniexCandle => poloCandle}
import io.liquirium.core.helpers.CoreHelpers.{dec, milli}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Duration

class PoloniexModelConverterTest_Candles extends PoloniexModelConverterTest {

  private def convert(c: PoloniexCandle) =
    converter().convertCandle(c)

  test("the startTime is taken as the start time") {
    convert(poloCandle(startTime = milli(3))).startTime shouldEqual milli(3)
  }

  test("the length is set to the given duration") {
    convert(poloCandle(Duration.ofMinutes(1))).length shouldEqual Duration.ofMinutes(1)
  }

  test("prices are just transferred to the result candle") {
    convert(poloCandle(open = dec(11))).open shouldEqual dec(11)
    convert(poloCandle(close = dec(12))).close shouldEqual dec(12)
    convert(poloCandle(high = dec(13))).high shouldEqual dec(13)
    convert(poloCandle(low = dec(14))).low shouldEqual dec(14)
  }

  test("the quote volume is set to the polo candle amount") {
    convert(poloCandle(amount = BigDecimal("1.23"))).quoteVolume shouldBe 1.23
  }
}
