package io.liquirium.connect.binance

import io.liquirium.connect.binance.BinanceHttpRequest.SignedDelete
import io.liquirium.connect.binance.helpers.BinanceTestHelpers
import io.liquirium.helpers.JsonTestHelper.json
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.{contain, matchPattern}

import scala.concurrent.Future

class BinanceRestApiTest_CancelOrder extends BinanceRestApiTest {

  private def cancelOrder
  (
    symbol: String = "",
    orderId: String = "",
  ): Future[BinanceOrder] =
    api.sendRequest(BinanceRestApi.CancelOrderRequest(
      symbol = symbol,
      orderId = orderId,
    ))

  test("it sends a private delete request to the correct endpoint") {
    cancelOrder()
    captureRequest() should matchPattern { case SignedDelete("/api/v3/order", _) => }
  }

  test("symbol and order id are passed on to the request") {
    cancelOrder(symbol = "DEFHIJ", orderId = "8765")
    val params = captureRequest().params
    params should contain ("symbol", "DEFHIJ")
    params should contain ("orderId", "8765")
  }

  test("the response is converted to an order") {
    val f = cancelOrder()
    jsonConverter.convertSingleOrder(json(123)) returns BinanceTestHelpers.order("123")
    baseService.completeNext(json(123))
    f.value.get.get shouldEqual BinanceTestHelpers.order("123")
  }

}
