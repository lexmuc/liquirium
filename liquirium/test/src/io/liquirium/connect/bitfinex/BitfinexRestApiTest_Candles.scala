package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexCandleLength.oneHour
import io.liquirium.connect.bitfinex.BitfinexRestApi.GetCandles
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.candle
import io.liquirium.core.helpers.CoreHelpers.{ex, milli}
import io.liquirium.helpers.JsonTestHelper.json
import io.liquirium.util.ResultOrder
import play.api.libs.json.JsValue

import java.time.Instant
import scala.util.{Failure, Success}

class BitfinexRestApiTest_Candles extends BitfinexRestApiTest {

  private def getCandles(
    symbol: String = "tABCDDEF",
    candleLength: BitfinexCandleLength = oneHour,
    limit: Option[Int] = None,
    from: Option[Instant] = None,
    until: Option[Instant] = None,
    order: ResultOrder = ResultOrder.DescendingOrder,
  ) = api.sendRequest(GetCandles(symbol = symbol, candleLength, limit, from = from, until = until, sort = order))

  private def fakeConversion(json: JsValue, candles: BitfinexCandle*) =
    jsonConverter.convertCandles(json) returns candles.toSeq

  private def fakeConversionFailure(t: Throwable) = jsonConverter.convertCandles(*) throws t

  test("requesting candles yields a get request with candle length code and symbol encoded in the path") {
    getCandles("tIOTBTC", BitfinexCandleLength("123A", 123))
    captureRequest() should matchPattern { case PublicBitfinexGetRequest("candles/trade:123A:tIOTBTC/hist", _) => }
  }

  test("the sort parameter is set to 1 for an ascending order, -1 for descending") {
    getCandles(order = ResultOrder.AscendingOrder)
    captureRequest().params.filter(_._1 == "sort") shouldEqual Seq(("sort", "1"))
    getCandles(order = ResultOrder.DescendingOrder)
    captureRequest().params.filter(_._1 == "sort") shouldEqual Seq(("sort", "-1"))
  }

  test("an optional limit is passed as a query parameter if set") {
    getCandles(limit = Some(123))
    captureRequest().params should contain("limit", "123")
  }

  test("optional 'from' parameter is passed in the query as start in milliseconds") {
    getCandles(from = Some(milli(12345)))
    captureRequest().params should contain("start", "12345")
  }

  test("optional 'until' parameter is passed in the query as end in milliseconds minus 1 millisecond") {
    getCandles(until = Some(milli(23456)))
    captureRequest().params should contain("end", "23455")
  }

  test("optional parameters can be combined") {
    getCandles(limit = Some(333), from = Some(milli(345)))
    captureRequest().params should contain allOf(("limit", "333"), ("start", "345"))
  }

  test("it parses the candles with the json converter") {
    val f = getCandles()
    fakeConversion(json(123), candle(1), candle(2))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Success(Seq(candle(1), candle(2)))
  }

  test("the candles are returned in the given order regardless of timestamps") {
    val f = getCandles()
    fakeConversion(json(123), candle(milli(1)), candle(milli(3)), candle(milli(2)))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Success(Seq(candle(milli(1)), candle(milli(3)), candle(milli(2))))
  }

  test("an error is returned when the json can't be parsed") {
    val f = getCandles()
    fakeConversionFailure(ex("ouch"))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Failure(BitfinexApiError.failedJsonConversion(json(123), ex("ouch")))
  }

  test("api errors are just forwarded") {
    val f = getCandles()
    baseService.failNext(ex("fail!"))
    f.value.get shouldEqual Failure(ex("fail!"))
  }

}
