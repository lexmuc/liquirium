package io.liquirium.connect.binance

import io.liquirium.connect.binance.BinanceHttpRequest.SignedPost
import io.liquirium.connect.binance.helpers.BinanceTestHelpers
import io.liquirium.core.Side
import io.liquirium.core.helper.CoreHelpers.dec
import io.liquirium.helper.JsonTestHelper.json

import scala.concurrent.Future

class BinanceRestApiTest_NewOrder extends BinanceRestApiTest {

  private def placeOrder
  (
    side: Side = Side.Buy,
    symbol: String = "",
    quantity: BigDecimal = BigDecimal(1),
    price: BigDecimal = BigDecimal(1),
    orderType: String = "LIMIT"
  ): Future[BinanceOrder] =
    api.sendRequest(BinanceRestApi.NewOrderRequest(
      side = side,
      symbol = symbol,
      quantity = quantity,
      price = price,
      orderType = orderType
    ))

  test("it sends a private post request to the correct endpoint") {
    placeOrder()
    captureRequest() should matchPattern { case SignedPost("/api/v3/order", _) => }
  }

  test("it assigns all the parameters as given") {
    placeOrder(
      side = Side.Buy,
      symbol = "ABCDEF",
      quantity = dec("3.5"),
      price = dec("4.5"),
      orderType = "LIMIT_MAKER",
    )
    val req = captureRequest()
    req.params should contain ("side", "BUY")
    req.params should contain ("symbol", "ABCDEF")
    req.params should contain ("quantity", "3.5")
    req.params should contain ("price", "4.5")
    req.params should contain ("type", "LIMIT_MAKER")

    reset()
    placeOrder(side = Side.Sell)
    captureRequest().params should contain ("side", "SELL")
  }

  test("price and quantity may be very small but are never sent in scientific notation") {
    placeOrder(
      quantity = dec("0.00000005"),
      price = dec("0.00000006"),
    )
    val req = captureRequest()
    req.params should contain ("quantity", "0.00000005")
    req.params should contain ("price", "0.00000006")
  }

  test("the time in force is set to GTC for LIMIT orders and omitted for LIMIT_MAKER orders") {
    placeOrder(orderType = "LIMIT")
    captureRequest().params should contain ("timeInForce", "GTC")

    reset()
    placeOrder(orderType = "LIMIT_MAKER")
    captureRequest().params.map(_._1).toSet should not contain "timeInForce"
  }

  test("the response type is set to result") {
    placeOrder()
    captureRequest().params should contain ("newOrderRespType", "RESULT")
  }

  test("the response is converted to an order") {
    val f = placeOrder()
    jsonConverter.convertSingleOrder(json(123)) returns BinanceTestHelpers.order("123")
    baseService.completeNext(json(123))
    f.value.get.get shouldEqual BinanceTestHelpers.order("123")
  }

}
