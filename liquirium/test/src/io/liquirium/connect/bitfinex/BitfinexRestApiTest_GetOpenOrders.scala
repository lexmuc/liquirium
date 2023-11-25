package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexRestApi.GetOpenOrders
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.order
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.JsValue

class BitfinexRestApiTest_GetOpenOrders extends TestWithMocks {

  private def getOrders(
    symbol: Option[String] = None
  ) = GetOpenOrders(
    symbol = symbol
  )

  val jsonConverter: BitfinexJsonConverter = mock[BitfinexJsonConverter]

  private def fakeSeqConversion(json: JsValue, orders: Seq[BitfinexOrder]) =
    jsonConverter.convertOrders(json) returns orders

  test("the http request is a private GET request") {
    getOrders().httpRequest(jsonConverter) shouldBe a[PrivateBitfinexPostRequest]
  }

  test("the http request has the correct path") {
    getOrders().httpRequest(jsonConverter).lastPathSegment shouldEqual "auth/r/orders/"
    getOrders(symbol = Some("ABC")).httpRequest(jsonConverter).lastPathSegment shouldEqual "auth/r/orders/ABC"
  }

  test("the result is parsed as a sequence of orders") {
    val response = json(123)
    fakeSeqConversion(json(123), Seq(order(1), order(2)))
    getOrders().convertResponse(response, jsonConverter) shouldEqual Seq(order(1), order(2))
  }

}
