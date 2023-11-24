package io.liquirium.connect.binance

import io.liquirium.core.helpers.CoreHelpers.{dec, milli, sec}
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.{candle => bc}

import java.time.Duration

class BinanceModelConverterTest_Candle extends BinanceModelConverterTest {

  private def convert(c: BinanceCandle) = converter().convertCandle(c)

  test("the open time is taken as the start time") {
    convert(bc(openTime = sec(3))).startTime shouldEqual sec(3)
  }

  test("the length is set as the difference of close and open time plus 1 milli") {
    convert(bc(openTime = milli(100), closeTime = milli(199) )).length shouldEqual Duration.ofMillis(100)
  }

  test("prices are just transferred to the result candle") {
    convert(bc(open = dec(11))).open shouldEqual dec(11)
    convert(bc(close = dec(12))).close shouldEqual dec(12)
    convert(bc(high = dec(13))).high shouldEqual dec(13)
    convert(bc(low = dec(14))).low shouldEqual dec(14)
  }

  test("the quote volume is the given quote asset volume") {
    convert(bc(quoteAssetVolume = 12.3)).quoteVolume shouldBe 12.3
  }
  
}
