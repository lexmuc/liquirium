package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.GetTradeHistory
import io.liquirium.connect.coinbase.CoinbaseHttpRequest.PrivateGet
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{coinbaseTrade => trade}
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.{JsObject, JsString, JsValue}

import java.time.Instant

class CoinbaseApiRequestTest_GetTradeHistory extends TestWithMocks {

  private def getTrades(
    orderId: Option[String] = None,
    productId: Option[String] = None,
    startSequenceTimestamp: Option[Instant] = None,
    endSequenceTimestamp: Option[Instant] = None,
    limit: Option[Long] = None,
  ) = GetTradeHistory(
    orderId = orderId,
    productId = productId,
    startSequenceTimestamp = startSequenceTimestamp,
    endSequenceTimestamp = endSequenceTimestamp,
    limit = limit
  )

  val jsonConverter: CoinbaseJsonConverter = mock[CoinbaseJsonConverter]

  private def fakeSeqConversion(json: JsValue, trades: Seq[CoinbaseTrade]) =
    jsonConverter.convertTrades(json) returns trades

  test("the http request is a private GET request") {
    getTrades().httpRequest shouldBe a[PrivateGet]
  }

  test("the order and product id are set as a parameter when given") {
    getTrades(orderId = Some("123")).httpRequest.params should contain("order_id", "123")
    getTrades(productId = Some("BTC-USD")).httpRequest.params should contain("product_id", "BTC-USD")
  }

  test("the start and end sequence time are set when given") {
    val r = getTrades(startSequenceTimestamp = Some(sec(1)), endSequenceTimestamp = Some(sec(2)))
    r.httpRequest.params should contain("start_sequence_timestamp", "1970-01-01T00:00:01Z")
    r.httpRequest.params should contain("end_sequence_timestamp", "1970-01-01T00:00:02Z")
  }

  test("a limit is set as a parameter when given") {
    getTrades(limit = Some(500)).httpRequest.params should contain("limit", "500")
  }

  test("the result is parsed as a sequence of trades") {
    val response = JsObject(Seq("fills" -> json(123), "cursor" -> JsString("")))
    fakeSeqConversion(json(123), Seq(trade("X"), trade("Y")))
    getTrades().convertResponse(response, jsonConverter) shouldEqual Seq(trade("X"), trade("Y"))
  }

  test("an exception is thrown when the response has a 'cursor' property") {
    val response = JsObject(Seq("fills" -> json(123), "cursor" -> JsString("789100")))
    fakeSeqConversion(json(123), Seq())
    an[Exception] shouldBe thrownBy(getTrades().convertResponse(response, jsonConverter))
  }

}
