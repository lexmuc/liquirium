package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexRestApi.GetOrderHistory
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.order
import io.liquirium.core.helpers.CoreHelpers.{ex, milli}
import io.liquirium.helpers.JsonTestHelper._
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.matchPattern
import play.api.libs.json.JsValue

import java.time.Instant
import scala.util.{Failure, Success}

class BitfinexRestApiTest_OrderHistory extends BitfinexRestApiTest {

  private def getOrderHistory(
    symbol: Option[String] = None,
    from: Option[Instant] = None,
    until: Option[Instant] = None,
    limit: Int = 1,
  ) = api.sendRequest(GetOrderHistory(symbol = symbol, from = from, until = until, limit = limit))

  private def fakeConversion(json: JsValue, orders: BitfinexOrder*) = jsonConverter.convertOrders(json) returns orders.toSeq

  private def fakeConversionFailure(t: Throwable) = jsonConverter.convertOrders(*) throws t

  test("requesting new orders yields a post request with the correct path") {
    getOrderHistory(symbol = None)
    captureRequest() should matchPattern { case PrivateBitfinexPostRequest("auth/r/orders/hist", _, _) => }
  }

  test("when a symbol is set, it is part of the path") {
    getOrderHistory(symbol = Some("xyz"))
    captureRequest().lastPathSegment shouldEqual "auth/r/orders/xyz/hist"
  }

  test("the limit is set as given") {
    getOrderHistory(None, None, None, limit = 123)
    captureRequest().params should contain("limit", "123")
  }

  test("adding start or end timestamps sets the respective parameters on the request (-1 for exclusive end)") {
    getOrderHistory(from = Some(milli(123)), until = Some(milli(234)))
    captureRequest().params should contain allOf(("start", "123"), ("end", "233"))
  }

  test("orders are converted with the given converter before they are returned") {
    val f = getOrderHistory()
    fakeConversion(json(123), order(1), order(2), order(3))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Success(Seq(order(1), order(2), order(3)))
  }

  test("an error is returned when the json cannot be parsed") {
    val f = getOrderHistory()
    fakeConversionFailure(ex("ouch"))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Failure(BitfinexApiError.failedJsonConversion(json(123), ex("ouch")))
  }

  test("api errors are just forwarded") {
    val f = getOrderHistory()
    baseService.failNext(ex("fail!"))
    f.value.get shouldEqual Failure(ex("fail!"))
  }

}
