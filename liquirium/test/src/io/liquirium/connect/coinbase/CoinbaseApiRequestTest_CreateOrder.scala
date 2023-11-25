package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.CreateOrder
import io.liquirium.connect.coinbase.CoinbaseHttpRequest.PrivatePost
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{coinbaseCreateOrderResponseFailure => failure, coinbaseCreateOrderResponseSuccess => success}
import io.liquirium.core.Side
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.{JsBoolean, JsString, JsValue}

class CoinbaseApiRequestTest_CreateOrder extends TestWithMocks {

  val jsonConverter: CoinbaseJsonConverter = mock[CoinbaseJsonConverter]

  private def fakeConversion(json: JsValue, createOrderResponse: CoinbaseCreateOrderResponse) =
    jsonConverter.convertCreateOrderResponse(json) returns createOrderResponse

  private def createOrder(
    side: Side = Side.Sell,
    productId: String = "BTC-USD",
    clientOrderId: String = "111-222-333",
    baseSize: BigDecimal = dec("1"),
    limitPrice: BigDecimal = dec("1"),
    postOnly: Boolean = false,
  ) = CreateOrder(
    side = side,
    productId = productId,
    clientOrderId = clientOrderId,
    baseSize = baseSize,
    limitPrice = limitPrice,
    postOnly = postOnly,
  )

  test("the http request is a private POST request") {
    createOrder().httpRequest shouldBe a[PrivatePost]
  }

  test("the http request has the correct path") {
    createOrder().httpRequest.path shouldEqual "/api/v3/brokerage/orders"
  }

  test("the side is encoded in the body") {
    (createOrder(side = Side.Sell).httpRequest.body \ "side").get shouldEqual JsString("SELL")
  }

  test("the product id and client order id is encoded in the body") {
    (createOrder(productId = "ETH-USD").httpRequest.body \ "product_id").get shouldEqual
      JsString("ETH-USD")
    (createOrder(clientOrderId = "111-222-333").httpRequest.body \ "client_order_id").get shouldEqual
      JsString("111-222-333")
  }

  test("the base size and limit price are encoded in the body") {
    (((createOrder(baseSize = dec("1.23")).httpRequest.body \ "order_configuration") \ "limit_limit_gtc") \ "base_size").get shouldEqual JsString("1.23")
    (((createOrder(limitPrice = dec("1.23")).httpRequest.body \ "order_configuration") \ "limit_limit_gtc") \ "limit_price").get shouldEqual JsString("1.23")
  }

  test("post only is encoded in the body") {
    (((createOrder(postOnly = true).httpRequest.body \ "order_configuration") \ "limit_limit_gtc") \ "post_only").get shouldEqual JsBoolean(true)
    (((createOrder(postOnly = false).httpRequest.body \ "order_configuration") \ "limit_limit_gtc") \ "post_only").get shouldEqual JsBoolean(false)
  }

  test("the result is parsed as a coinbase create order response") {
    fakeConversion(json(123), success("s"))
    fakeConversion(json(456), failure("f"))
    createOrder().convertResponse(json(123), jsonConverter) shouldEqual success("s")
    createOrder().convertResponse(json(456), jsonConverter) shouldEqual failure("f")
  }

}
