package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{coinbaseCandle => cc}
import io.liquirium.core.helpers.CoreHelpers.{dec, milli, millis, sec}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Duration


class CoinbaseModelConverterTest_Candle extends CoinbaseModelConverterTest {

  private def convert(c: CoinbaseCandle, candleLength: Duration = millis(0)) =
    converter().convertCandle(c, candleLength)

  test("the open time is taken as the start time") {
    convert(cc(start = sec(3))).startTime shouldEqual sec(3)
  }

  test("the length is set as the duration the function was called with") {
    convert(cc(start = milli(100)), millis(3600)).length shouldEqual Duration.ofMillis(3600)
  }

  test("prices are just transferred to the result candle") {
    convert(cc(open = dec(11))).open shouldEqual dec(11)
    convert(cc(close = dec(12))).close shouldEqual dec(12)
    convert(cc(high = dec(13))).high shouldEqual dec(13)
    convert(cc(low = dec(14))).low shouldEqual dec(14)
  }

  test("the quote volume is the given volume") {
    convert(cc(volume = 12.3)).quoteVolume shouldBe 12.3
  }

}
