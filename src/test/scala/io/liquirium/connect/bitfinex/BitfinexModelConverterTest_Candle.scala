package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.{candle => bitfinexCandle}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.MarketHelpers.eid

import java.time.Duration

class BitfinexModelConverterTest_Candle extends BasicTest {

  private def convert(c: BitfinexCandle, length: Duration = Duration.ofSeconds(1)) =
    new BitfinexModelConverter(eid(0)).convertCandle(c, length)

  test("the timestamp is taken as the start time") {
    convert(bitfinexCandle(timestamp = sec(3))).startTime shouldEqual sec(3)
  }

  test("the length is set to the given duration") {
    convert(bitfinexCandle(123), Duration.ofSeconds(17)).length shouldEqual Duration.ofSeconds(17)
  }

  test("prices are just transferred to the result candle") {
    convert(bitfinexCandle(open = dec(11))).open shouldEqual dec(11)
    convert(bitfinexCandle(close = dec(12))).close shouldEqual dec(12)
    convert(bitfinexCandle(high = dec(13))).high shouldEqual dec(13)
    convert(bitfinexCandle(low = dec(14))).low shouldEqual dec(14)
  }
  
  test("the quote volume is calculated based on the average of high and low") {
    convert(bitfinexCandle(high = dec(5),low = dec(3), volume = 10)).quoteVolume shouldBe 40
  }

}
