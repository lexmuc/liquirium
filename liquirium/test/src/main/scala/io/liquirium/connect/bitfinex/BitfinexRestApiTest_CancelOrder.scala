package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.{JsValue, Json}

class BitfinexRestApiTest_CancelOrder extends BitfinexRestApiTest {

  private def fakeConversion(json: JsValue, result: BitfinexOrder) =
    jsonConverter.convertSingleOrder(json) returns result

  private def cancelOrder(
    id: Int = 123
  ): BitfinexRestApi.CancelOrder = BitfinexRestApi.CancelOrder(
    id = id
  )

  test("the http request is a private POST request") {
    cancelOrder().httpRequest(jsonConverter) shouldBe a[PrivateBitfinexPostRequest]
  }

  test("the http request has the correct path") {
    val pp = cancelOrder().httpRequest(jsonConverter)
    cancelOrder().httpRequest(jsonConverter).lastPathSegment shouldEqual "auth/w/order/cancel"
  }

  test("the id is encoded in the parameters") {
    cancelOrder(id = 456).httpRequest(jsonConverter).params should contain("id", "456")
  }

  test("the response is converted to an order") {
    val response = Json.parse(
      s"""
         |[1567590617.442,
         |"on-req",
         |null,
         |null,
         |${ json(123).toString() },
         |null,
         |"SUCCESS",
         |"Submitted for cancellation"]
         |""".stripMargin)
    fakeConversion(json(123), BitfinexTestHelpers.order(123))
    cancelOrder().convertResponse(response, jsonConverter) shouldEqual BitfinexTestHelpers.order(123)
  }

}
