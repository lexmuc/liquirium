package io.liquirium.connect.binance

import io.liquirium.connect.AscendingHistoryBatch
import io.liquirium.connect.binance.BinanceHttpRequest.PublicGet
import io.liquirium.connect.binance.BinanceRestApi.CandlesRequest
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.candle
import io.liquirium.core.helper.CoreHelpers.{ex, milli, secs}
import io.liquirium.helper.JsonTestHelper.json
import play.api.libs.json.JsValue

import java.time.Instant
import scala.util.{Failure, Success}

class BinanceRestApiTest_Candles extends BinanceRestApiTest {

  private def getCandles(symbol: String = "tABCDDEF",
                 resolution: BinanceCandleResolution = BinanceCandleResolution.oneDay,
                 limit: Option[Int] = None,
                 from: Option[Instant] = None,
                 until: Option[Instant] = None) =
    api.sendRequest(CandlesRequest(symbol = symbol, resolution, limit, from = from, until = until))

  private def fakeConversion(json: JsValue, candles: BinanceCandle*) =
    jsonConverter.convertCandles(json) returns candles.toSeq

  private def fakeConversionFailure(t: Throwable) = jsonConverter.convertCandles(*) throws t

  test("requesting candles yields a get request with the proper url") {
    getCandles("IOTBTC", BinanceCandleResolution("XYZ", secs(1)))
    captureRequest() should matchPattern { case PublicGet("/api/v3/klines", _) => }
  }

  test("the interval code and symbol are encoded in the parameters") {
    getCandles("IOTBTC", BinanceCandleResolution("XYZ", secs(1)))
    captureRequest().params should contain allElementsOf Seq(("symbol", "IOTBTC"), ("interval", "XYZ"))
  }

  test("an optional limit is passed as a query parameter if set") {
    getCandles(limit = Some(123))
    captureRequest().params should contain("limit", "123")
  }

  test("an optional 'from' parameter is passed in the query as start in milliseconds") {
    getCandles(from = Some(milli(12345)))
    captureRequest().params should contain("startTime", "12345")
  }

  test("an optional 'until' parameter is passed in the query as end in milliseconds minus 1 millisecond") {
    getCandles(until = Some(milli(23456)))
    captureRequest().params should contain("endTime", "23455")
  }

  test("optional parameters can be combined") {
    getCandles(limit = Some(333), from = Some(milli(345)))
    captureRequest().params should contain allOf(("limit", "333"), ("startTime", "345"))
  }

    test("it parses the candles with the json converter and returns them as a filled ascending batch") {
      val f = getCandles()
      fakeConversion(json(123), candle(1), candle(2))
      baseService.completeNext(json(123))
      f.value.get shouldEqual Success(AscendingHistoryBatch(Seq(candle(1), candle(2))))
    }

  test("an error is returned when the json can't be parsed") {
    val f = getCandles()
    fakeConversionFailure(ex("ouch"))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Failure(BinanceApiError.failedJsonConversion(json(123), ex("ouch")))
  }

  test("api errors are just forwarded") {
    val f = getCandles()
    baseService.failNext(ex("fail!"))
    f.value.get shouldEqual Failure(ex("fail!"))
  }

}
