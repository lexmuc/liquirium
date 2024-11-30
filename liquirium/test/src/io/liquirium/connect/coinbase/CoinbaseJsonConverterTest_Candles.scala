package io.liquirium.connect.coinbase

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.{dec, milli}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsArray, JsValue, Json}

class CoinbaseJsonConverterTest_Candles extends BasicTest {

  private def convertCandle(json: JsValue) = new CoinbaseJsonConverter().convertSingleCandle(json)

  private def convertCandles(json: JsValue) = new CoinbaseJsonConverter().convertCandles(json)

  private def candleJson(
    start: String = "0",
    low: String = "1",
    high: String = "1",
    open: String = "1",
    close: String = "1",
    volume: String = "1",
  ) =
    Json.parse(
      s"""{
         |"start":"$start",
         |"low":"$low",
         |"high":"$high",
         |"open":"$open",
         |"close":"$close",
         |"volume":"$volume"
         |}""".stripMargin)


  test("it interprets the timestamp as milliseconds") {
    convertCandle(candleJson(start = "1234")).start shouldEqual milli(1234)
  }

  test("it parses the price fields") {
    val cs = candleJson(open = "1.2", close = "2.3", high = "3.4", low = "4.5")
    convertCandle(cs).open shouldEqual dec("1.2")
    convertCandle(cs).close shouldEqual dec("2.3")
    convertCandle(cs).high shouldEqual dec("3.4")
    convertCandle(cs).low shouldEqual dec("4.5")
  }

  test("it just parses the volume as a big decimal") {
    convertCandle(candleJson(volume = "12.34")).volume shouldEqual dec("12.34")
  }

  test("it can parse serveral candles in an array") {
    convertCandles(JsArray(Seq(candleJson(start = "12"), candleJson(start = "34"))))
      .map(_.start) shouldEqual Seq(milli(12), milli(34))
  }

}
