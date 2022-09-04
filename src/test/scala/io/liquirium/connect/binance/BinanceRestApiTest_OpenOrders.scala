package io.liquirium.connect.binance

import io.liquirium.connect.binance.BinanceHttpRequest.SignedGet
import io.liquirium.connect.binance.BinanceRestApi.OpenOrdersRequest
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.order
import io.liquirium.core.helper.CoreHelpers.ex
import io.liquirium.helper.JsonTestHelper.json
import play.api.libs.json.JsValue

import scala.util.{Failure, Success}

class BinanceRestApiTest_OpenOrders extends BinanceRestApiTest {

  private def getOpenOrders(symbol: Option[String] = None ) =
    api.sendRequest(OpenOrdersRequest(symbol = symbol))

  private def fakeConversion(json: JsValue, orders: BinanceOrder*) =
    jsonConverter.convertOrders(json) returns orders.toSeq

  private def fakeConversionFailure(t: Throwable) = jsonConverter.convertOrders(*) throws t

  test("requesting orders yields a get request with the proper url") {
    getOpenOrders()
    captureRequest() should matchPattern { case SignedGet("/api/v3/openOrders", _) => }
  }

  test("no parameters are sent when there is no symbol") {
    getOpenOrders(symbol = None)
    captureRequest().params shouldEqual Seq()
  }

  test("the symbol is encoded in the request as a get parameter if set") {
    getOpenOrders(symbol = Some("ABCDEF"))
    captureRequest().params shouldEqual Seq(("symbol", "ABCDEF"))
  }

  test("it parses the orders with the json converter and returns them as a set") {
    val f = getOpenOrders()
    fakeConversion(json(123), order("1"), order("2"))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Success(Set(order("1"), order("2")))
  }

  test("an error is returned when the json can't be parsed") {
    val f = getOpenOrders()
    fakeConversionFailure(ex("ouch"))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Failure(BinanceApiError.failedJsonConversion(json(123), ex("ouch")))
  }

  test("api errors are just forwarded") {
    val f = getOpenOrders()
    baseService.failNext(ex("fail!"))
    f.value.get shouldEqual Failure(ex("fail!"))
  }

}
