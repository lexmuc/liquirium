package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest.GetOpenOrders
import io.liquirium.connect.poloniex.PoloniexHttpRequest.PrivateGet
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.{poloniexOrder => order}
import io.liquirium.core.Side
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.helpers.JsonTestHelper.json
import play.api.libs.json.JsValue


class PoloniexApiRequestTest_GetOpenOrders extends TestWithMocks {

  private def getOrders(
    symbol: Option[String] = None,
    side: Option[Side] = None,
    from: Option[Long] = None,
    direction: Option[String] = None,
    limit: Option[Int] = None,
  ) = GetOpenOrders(
    symbol = symbol,
    side = side,
    from = from,
    direction = direction,
    limit = limit,
  )

  val jsonConverter: PoloniexJsonConverter = mock[PoloniexJsonConverter]

  private def fakeSeqConversion(json: JsValue, orders: Seq[PoloniexOrder]) =
    jsonConverter.convertOrders(json) returns orders

  test("the http request is a private GET request") {
    getOrders().httpRequest shouldBe a[PrivateGet]
  }

  test("the http request has the correct path") {
    getOrders().httpRequest.path shouldEqual "/orders"
  }

  test("a symbol is set as a parameter when given") {
    getOrders(symbol = Some("ETHW_USDT")).httpRequest.params should contain("symbol", "ETHW_USDT")
  }

  test("a side is set as a parameter when given") {
    getOrders(side = Some(Side.Buy)).httpRequest.params should contain("side", "BUY")
  }

  test("from is set as a parameter when given") {
    getOrders(from = Some(123456789)).httpRequest.params should contain("from", "123456789")
  }

  test("the direction is set as a parameter when given") {
    getOrders(direction = Some("PRE")).httpRequest.params should contain("direction", "PRE")
  }

  test("the limit is set as a parameter when given") {
    getOrders(limit = Some(200)).httpRequest.params should contain("limit", "200")
  }

  test("the result is parsed as a sequence of orders") {
    fakeSeqConversion(json(123), Seq(order("a"), order("b")))
    getOrders().convertResponse(json(123), jsonConverter) shouldEqual Seq(order("a"), order("b"))
  }

}
