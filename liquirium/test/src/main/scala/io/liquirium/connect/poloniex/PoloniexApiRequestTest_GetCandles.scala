package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest.GetCandles
import io.liquirium.connect.poloniex.PoloniexHttpRequest.PublicGet
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.poloniexCandle
import io.liquirium.core.helpers.CoreHelpers.milli
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.JsValue

import java.time.Instant

class PoloniexApiRequestTest_GetCandles extends TestWithMocks {

  val jsonConverter: PoloniexJsonConverter = mock[PoloniexJsonConverter]

  private def getCandles(
    symbol: String = "ETHW_USDT",
    interval: PoloniexCandleLength = PoloniexCandleLength.fiveMinutes,
    limit: Option[Int] = None,
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None,
  ) =
    GetCandles(
      symbol = symbol,
      interval = interval,
      limit = limit,
      startTime = startTime,
      endTime = endTime,
    )

  private def fakeConversion(json: JsValue, candles: PoloniexCandle*) =
    jsonConverter.convertCandles(json) returns candles.toSeq

  test("the http request is a public GET request") {
    getCandles().httpRequest shouldBe a[PublicGet]
  }

  test("the http request has the correct path including the symbol") {
    getCandles(symbol = "ABCDEF").httpRequest.path shouldEqual "/markets/ABCDEF/candles"
  }

  test("the candle length (interval) is encoded in the parameters") {
    getCandles(interval = PoloniexCandleLength.thirtyMinutes).httpRequest.params should contain("interval", "MINUTE_30")
  }

  test("the 'limit' parameter is correctly encoded in the parameters") {
    getCandles(limit = Option(800)).httpRequest.params should contain("limit", "800")
  }

  test("the 'startTime' parameter is passed in the query as milliseconds") {
    getCandles(startTime = Option(milli(8800))).httpRequest.params should contain("startTime", "8800")
  }

  test("the 'endTime' parameter is passed in the query as milliseconds") {
    getCandles(endTime = Option(milli(12345))).httpRequest.params should contain("endTime", "12345")
  }

  test("the response is just parsed as poloniex candles") {
    fakeConversion(json(123), poloniexCandle(1), poloniexCandle(2))
    getCandles().convertResponse(json(123), jsonConverter) shouldEqual Seq(poloniexCandle(1), poloniexCandle(2))
  }

}
