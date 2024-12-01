package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.GetCandles
import io.liquirium.connect.coinbase.CoinbaseHttpRequest.PrivateGet
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.coinbaseCandle
import io.liquirium.core.helpers.CoreHelpers.milli
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import org.mockito.Mockito.mock
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.{a, an, convertToAnyShouldWrapper, thrownBy}
import play.api.libs.json.{JsObject, JsValue}

import java.time.Instant

class CoinbaseApiRequestTest_GetCandles extends TestWithMocks {

  val jsonConverter: CoinbaseJsonConverter = mock(classOf[CoinbaseJsonConverter])

  private def getCandles(
    productId: String = "ETHW_USDT",
    start: Instant = milli(0),
    end: Instant = milli(1000),
    granularity: CoinbaseCandleLength = CoinbaseCandleLength.fiveMinutes,
  ) =
    GetCandles(
      productId = productId,
      start = start,
      end = end,
      granularity = granularity,
    )

  private def fakeConversion(json: JsValue, candles: CoinbaseCandle*) =
    jsonConverter.convertCandles(json) returns candles.toSeq

  test("the http request is a private GET request") {
    getCandles().httpRequest shouldBe a[PrivateGet]
  }

  test("the http request has the correct path") {
    getCandles(productId = "ABCDEF").httpRequest.path shouldEqual "/api/v3/brokerage/products/ABCDEF/candles"
  }

  test("the candle length (granularity) is encoded in the parameters") {
    getCandles(granularity = CoinbaseCandleLength.thirtyMinutes)
      .httpRequest.params should contain("granularity", "THIRTY_MINUTE")
  }

  test("the 'start' parameter is passed in the query as seconds") {
    getCandles(start = milli(8000)).httpRequest.params should contain("start", "8")
  }

  test("the 'end' parameter is passed in the query as seconds") {
    getCandles(end = milli(12000)).httpRequest.params should contain("end", "12")
  }

  test("the candles are parsed from the response 'candles' field") {
    val response = JsObject(Map("candles" -> json(123)))
    fakeConversion(json(123), coinbaseCandle(1), coinbaseCandle(2))
    getCandles().convertResponse(response, jsonConverter) shouldEqual Seq(coinbaseCandle(1), coinbaseCandle(2))
  }

  test("start and end time have been set in full seconds") {
    an[Exception] shouldBe thrownBy(getCandles(start = milli(6666)).httpRequest)
    an[Exception] shouldBe thrownBy(getCandles(end = milli(8888)).httpRequest)
  }

}
