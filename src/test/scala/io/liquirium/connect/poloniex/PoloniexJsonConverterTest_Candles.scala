package io.liquirium.connect.poloniex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.{dec, milli}
import play.api.libs.json.{JsArray, JsValue, Json}

class PoloniexJsonConverterTest_Candles extends BasicTest {

  private def convertCandle(json: JsValue) = new PoloniexJsonConverter().convertSingleCandle(json)

  private def convertCandles(json: JsValue) = new PoloniexJsonConverter().convertCandles(json)

  private def candleJson(
    low: String = "1",
    high: String = "1",
    open: String = "1",
    close: String = "1",
    amount: String = "1",
    quantity: String = "1",
    buyTakerAmount: String = "1",
    buyTakerQuantity: String = "1",
    tradeCount: Int = 1,
    ts: Long = 0,
    weightedAverage: String = "1",
    interval: String = "MINUTE_1",
    startTime: Long = 0,
    closeTime: Long = 60000,
  ) =
    Json.parse(
      s"""[
         "$low",
         "$high",
         "$open",
         "$close",
         "$amount",
         "$quantity",
         "$buyTakerAmount",
         "$buyTakerQuantity",
         $tradeCount,
         $ts,
         "$weightedAverage",
         "$interval",
         $startTime,
         $closeTime
         ]"""
    )

  test("it parses the price fields") {
    val cs = candleJson(
      low = "4.5",
      high = "3.4",
      open = "1.2",
      close = "2.3",
    )
    convertCandle(cs).low shouldEqual dec("4.5")
    convertCandle(cs).high shouldEqual dec("3.4")
    convertCandle(cs).open shouldEqual dec("1.2")
    convertCandle(cs).close shouldEqual dec("2.3")
  }

  test("it parses the volume, quote volume and weighted average as a big decimal") {
    val cs = candleJson(
      amount = "5.6",
      quantity = "6.7",
      weightedAverage = "7.8",
    )
    convertCandle(cs).amount shouldEqual dec("5.6")
    convertCandle(cs).quantity shouldEqual dec("6.7")
    convertCandle(cs).weightedAverage shouldEqual dec("7.8")
  }

  test("it parses the buyTakerAmount and buyTakerQuantity as a big decimal") {
    val cs = candleJson(
      buyTakerAmount = "5.6",
      buyTakerQuantity = "6.7",
    )
    convertCandle(cs).buyTakerAmount shouldEqual dec("5.6")
    convertCandle(cs).buyTakerQuantity shouldEqual dec("6.7")
  }

  test("it parses the trade count as integer") {
    val cs = candleJson(tradeCount = 111)
    convertCandle(cs).tradeCount shouldEqual 111
  }

  test("it parses the interval as poloniex candle length") {
    val cs = candleJson(interval = "MINUTE_5")
    convertCandle(cs).interval shouldEqual PoloniexCandleLength.fiveMinutes
  }

  test("it interprets the timestamps as milliseconds") {
    val cs = candleJson(
      startTime = 1234,
      ts = 1235,
      closeTime = 1236,
    )
    convertCandle(cs).startTime shouldEqual milli(1234)
    convertCandle(cs).ts shouldEqual milli(1235)
    convertCandle(cs).closeTime shouldEqual milli(1236)
  }

  test("it can parse several candles in an array") {
    convertCandles(JsArray(Seq(candleJson(startTime = 12), candleJson(startTime = 34))))
      .map(_.startTime) shouldEqual Seq(milli(12), milli(34))
  }

  //#? Könnte bei neuer Api anders sein?
  test("if the candle array consists of a single candle with date 0, the array is returned as empty") {
    convertCandles(JsArray(Seq(candleJson()))) shouldEqual Seq()
  }

}
