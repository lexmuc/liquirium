package io.liquirium.connect.binance

import io.liquirium.core.helper.BasicTest
import io.liquirium.core.helper.CoreHelpers.milli
import play.api.libs.json.{JsArray, JsValue, Json}

class BinanceJsonConverterTest_Candles extends BasicTest {

  private def convertCandle(json: JsValue) = new BinanceJsonConverter().convertSingleCandle(json)

  private def convertCandles(json: JsValue) = new BinanceJsonConverter().convertCandles(json)

  private def candleJson(openTime: Long = 0,
                         closeTime: Long = 1,
                         open: String = "1",
                         close: String = "1",
                         high: String = "1",
                         low: String = "1",
                         quoteAssetVolume: String = "1") =
    Json.parse(
      s"""[
    $openTime,
    "$open",
    "$high",
    "$low",
    "$close",
    "148976.11427815",
    $closeTime,
    "$quoteAssetVolume",
    308,
    "1756.87402397",
    "28.46694368",
    "17928899.62484339"
  ]""")

  test("it interprets the open and close time as milliseconds") {
    convertCandle(candleJson(openTime = 1234)).openTime shouldEqual milli(1234)
    convertCandle(candleJson(closeTime = 2345)).closeTime shouldEqual milli(2345)
  }

  test("it parses the price fields") {
    val cs = candleJson(open = "1.2", close = "2.3", high = "3.4", low = "4.5")
    convertCandle(cs).open shouldEqual BigDecimal("1.2")
    convertCandle(cs).close shouldEqual BigDecimal("2.3")
    convertCandle(cs).high shouldEqual BigDecimal("3.4")
    convertCandle(cs).low shouldEqual BigDecimal("4.5")
  }

  test("it just parses the quote asset volume as a big decimal") {
    convertCandle(candleJson(quoteAssetVolume = "12.34")).quoteAssetVolume shouldEqual BigDecimal("12.34")
  }

  test("it can parse serveral candles in an array") {
    convertCandles(JsArray(Seq(candleJson(openTime = 12), candleJson(openTime = 34))))
      .map(_.openTime) shouldEqual Seq(milli(12), milli(34))
  }

}
