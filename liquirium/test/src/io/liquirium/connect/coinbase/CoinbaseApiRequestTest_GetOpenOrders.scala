package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.GetOpenOrders
import io.liquirium.connect.coinbase.CoinbaseHttpRequest.PrivateGet
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{coinbaseOrder => order}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.{JsBoolean, JsObject, JsValue}

class CoinbaseApiRequestTest_GetOpenOrders extends TestWithMocks {

  private def getOrders(
    productId: Option[String] = None,
    limit: Int = 30,
  ) = GetOpenOrders(
    productId = productId,
    limit = limit,
  )

  val jsonConverter: CoinbaseJsonConverter = mock[CoinbaseJsonConverter]

  private def fakeSeqConversion(json: JsValue, orders: Seq[CoinbaseOrder]) =
    jsonConverter.convertOrders(json) returns orders

  test("the http request is a private GET request") {
    getOrders().httpRequest shouldBe a[PrivateGet]
  }

  test("the http request has the correct path") {
    getOrders().httpRequest.path shouldEqual "/api/v3/brokerage/orders/historical/batch"
  }

  test("a product id is set as a parameter when given") {
    getOrders(productId = Some("ETHW-USDT")).httpRequest.params should contain("product_id", "ETHW-USDT")
  }

  //  removed status parameter because of error - fetching only 'PENDING' orders doesn't seem to be possible
  //  test("the order status is always set as 'PENDING'") {
  //    getOrders().httpRequest.params should contain("order_status", "PENDING")
  //  }

  test("the limit is set as a parameter when given") {
    getOrders(limit = 200).httpRequest.params should contain("limit", "200")
  }

  test("the result is parsed as a sequence of orders") {
    val response = JsObject(Seq("orders" -> json(123), "has_next" -> JsBoolean(false)))
    fakeSeqConversion(json(123), Seq(order("a"), order("b")))
    getOrders().convertResponse(response, jsonConverter) shouldEqual Seq(order("a"), order("b"))
  }

  test("an exception is thrown when the 'has_next' entry on the response json is 'true'") {
    val response = JsObject(Seq("orders" -> json(123), "has_next" -> JsBoolean(true)))
    fakeSeqConversion(json(123), Seq())
    an[Exception] shouldBe thrownBy(getOrders().convertResponse(response, jsonConverter))
  }

}
