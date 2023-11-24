package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.milli
import play.api.libs.json.{JsArray, JsValue, Json}

class BitfinexJsonConverterTest_Candles extends BasicTest {

  private def convertCandle(json: JsValue) = new BitfinexJsonConverter().convertSingleCandle(json)

  private def convertCandles(json: JsValue) = new BitfinexJsonConverter().convertCandles(json)

  private def candleJson(
    timestamp: Long = 0,
    open: String = "1",
    close: String = "1",
    high: String = "1",
    low: String = "1",
    volume: String = "1",
  ) =
    Json.parse(s"""[$timestamp,$open, $close, $high, $low, $volume]""")

  test("it interprets the timestamp as milliseconds") {
    convertCandle(candleJson(timestamp = 1234)).timestamp shouldEqual milli(1234)
  }

  test("it parses the price fields") {
    val cs = candleJson(open = "1.2", close = "2.3", high = "3.4", low = "4.5")
    convertCandle(cs).open shouldEqual BigDecimal("1.2")
    convertCandle(cs).close shouldEqual BigDecimal("2.3")
    convertCandle(cs).high shouldEqual BigDecimal("3.4")
    convertCandle(cs).low shouldEqual BigDecimal("4.5")
  }

  test("it just parses the volume as a big decimal") {
    convertCandle(candleJson(volume = "12.34")).volume shouldEqual BigDecimal("12.34")
  }

  test("it can parse serveral candles in an array") {
    convertCandles(JsArray(Seq(candleJson(timestamp = 12), candleJson(timestamp = 34))))
      .map(_.timestamp) shouldEqual Seq(milli(12), milli(34))
  }

}
