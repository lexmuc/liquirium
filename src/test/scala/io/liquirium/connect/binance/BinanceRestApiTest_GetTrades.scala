package io.liquirium.connect.binance

import io.liquirium.connect.binance.BinanceHttpRequest.SignedGet
import io.liquirium.connect.binance.BinanceRestApi.GetTradesRequest
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.{trade => bt}
import io.liquirium.core.helpers.CoreHelpers.{ex, milli, millis}
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.JsValue

import java.time.Instant
import scala.util.{Failure, Success}


class BinanceRestApiTest_GetTrades extends BinanceRestApiTest {

  private def getTrades
  (
    symbol:String = "X",
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None,
    limit: Option[Int] = None
  ) =
    api.sendRequest(GetTradesRequest(symbol = symbol, startTime = startTime, endTime = endTime, limit = limit))

  private def fakeConversion(json: JsValue, trades: BinanceTrade*) =
    jsonConverter.convertTrades(json) returns trades.toSeq

  private def fakeConversionFailure(t: Throwable) = jsonConverter.convertTrades(*) throws t

  test("requesting orders yields a get request with the proper url") {
    getTrades()
    captureRequest() should matchPattern { case SignedGet("/api/v3/myTrades", _) => }
  }

  test("the symbol is included in the parameters") {
    getTrades(symbol = "ASDFJK")
    captureRequest().params should contain ("symbol", "ASDFJK")
  }

  test("start end end time are not set as params if not defined") {
    getTrades(startTime = None, endTime = None)
    val keys = captureRequest().params.map(_._1).toSet
    keys should not contain "startTime"
    keys should not contain "endTime"
  }

  test("start and end time are set converted to millis if defined") {
    getTrades(startTime = Some(milli(123)), endTime = Some(milli(234)))
    val params = captureRequest().params
    params should contain ("startTime", "123")
    params should contain ("endTime", "234")
  }

  test("the limit is not set if not defined") {
    getTrades(limit = None)
    val keys = captureRequest().params.map(_._1).toSet
    keys should not contain "limit"
  }

  test("the limit is set if defined") {
    getTrades(limit = Some(33))
    captureRequest().params should contain ("limit", "33")
  }

  test("it parses the orders with the json converter and returns them as a set") {
    val f = getTrades()
    fakeConversion(json(123), bt("1"), bt("2"))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Success(Seq(bt("1"), bt("2")))
  }

  test("an error is returned when the json can't be parsed") {
    val f = getTrades()
    fakeConversionFailure(ex("ouch"))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Failure(BinanceApiError.failedJsonConversion(json(123), ex("ouch")))
  }

  test("api errors are just forwarded") {
    val f = getTrades()
    baseService.failNext(ex("fail!"))
    f.value.get shouldEqual Failure(ex("fail!"))
  }

}
