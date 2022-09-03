package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest.CreateOrder
import io.liquirium.connect.poloniex.PoloniexHttpRequest.PrivatePost
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.poloniexCreateOrderResponse
import io.liquirium.core.Side
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.JsValue

class PoloniexApiRequestTest_CreateOrder extends TestWithMocks {

  val jsonConverter: PoloniexJsonConverter = mock[PoloniexJsonConverter]

  private def fakeConversion(json: JsValue, orderResponse: PoloniexCreateOrderResponse) = {
    jsonConverter.convertCreateOrderResponse(json) returns orderResponse
  }

  private def createOrder(
    symbol: String = "BTC-USD",
    side: Side = Side.Buy,
    timeInForce: Option[PoloniexTimeInForce] = None,
    `type`: Option[PoloniexOrderType] = None,
    accountType: Option[String] = None,
    price: Option[BigDecimal] = None,
    quantity: Option[BigDecimal] = None,
    amount: Option[BigDecimal] = None,
    clientOrderId: Option[String] = None,

  ): CreateOrder =
    CreateOrder(
      symbol = symbol,
      side = side,
      timeInForce = timeInForce,
      `type` = `type`,
      accountType = accountType,
      price = price,
      quantity = quantity,
      amount = amount,
      clientOrderId = clientOrderId
    )


  test("the http request is a private POST request") {
    createOrder().httpRequest shouldBe a[PrivatePost]
  }

  test("the http request has the correct path") {
    createOrder().httpRequest.path shouldEqual "/orders"
  }

  test("the symbol is encoded in the parameters") {
    createOrder(symbol = "ETH-EUR").httpRequest.params should contain("symbol", "ETH-EUR")
  }

  test("the side is encoded in the parameters") {
    createOrder(side = Side.Sell).httpRequest.params should contain("side", "SELL")
    createOrder(side = Side.Buy).httpRequest.params should contain("side", "BUY")
  }

  test("time in force is encoded in the parameters") {
    createOrder(timeInForce = Option(PoloniexTimeInForce.FOK)).httpRequest.params should contain("timeInForce", "FOK")
  }

  test("the type is encoded in the parameters") {
    createOrder(`type` = Option(PoloniexOrderType.LIMIT_MAKER)).httpRequest.params should contain("type", "LIMIT_MAKER")
  }

  test("the account type is encoded in the parameters") {
    createOrder(accountType = Option("SPOT")).httpRequest.params should contain("accountType", "SPOT")
  }

  test("the price is encoded in the parameters") {
    createOrder(price = Option(1.23)).httpRequest.params should contain("price", "1.23")
  }

  test("the quantity is encoded in the parameters") {
    createOrder(quantity = Option(1)).httpRequest.params should contain("quantity", "1")
  }

  test("the amount is encoded in the parameters") {
    createOrder(amount = Option(1)).httpRequest.params should contain("amount", "1")
  }

  test("the client order id is encoded in the parameters") {
    createOrder(clientOrderId = Option("abc")).httpRequest.params should contain("clientOrderId", "abc")
  }

  test("the result is parsed as a poloniex create order response") {
    fakeConversion(json(123), poloniexCreateOrderResponse("xyz"))
    createOrder().convertResponse(json(123), jsonConverter) shouldEqual poloniexCreateOrderResponse("xyz")
  }

}