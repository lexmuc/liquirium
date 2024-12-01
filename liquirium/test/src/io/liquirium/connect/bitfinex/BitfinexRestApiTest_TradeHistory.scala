package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexRestApi.GetTradeHistory
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.trade
import io.liquirium.core.helpers.CoreHelpers.{ex, milli}
import io.liquirium.helpers.JsonTestHelper._
import io.liquirium.util.ResultOrder
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.matchPattern
import play.api.libs.json.JsValue

import java.time.Instant
import scala.util.{Failure, Success}

class BitfinexRestApiTest_TradeHistory extends BitfinexRestApiTest {

  private def getTrades(
    symbol: Option[String] = None,
    from: Option[Instant] = None,
    until: Option[Instant] = None,
    limit: Int = 1,
    order: ResultOrder = ResultOrder.DescendingOrder,
  ) = api.sendRequest(GetTradeHistory(symbol = symbol, from = from, until = until, limit = limit, sort = order))

  private def fakeConversion(json: JsValue, trades: BitfinexTrade*) =
    jsonConverter.convertTrades(json) returns trades.toSeq

  private def fakeConversionFailure(t: Throwable) = jsonConverter.convertTrades(*) throws t

  test("requesting new trades yields a post request with the correct path") {
    getTrades(symbol = None)
    captureRequest() should matchPattern { case PrivateBitfinexPostRequest("auth/r/trades/hist", _, _) => }
  }

  test("when a symbol is set, it is part of the path") {
    getTrades(symbol = Some("xyz"))
    captureRequest() should matchPattern { case PrivateBitfinexPostRequest("auth/r/trades/xyz/hist", _, _) => }
  }

  test("the limit is set as given") {
    getTrades(None, None, None, limit = 1234)
    captureRequest().params should contain("limit", "1234")
  }

  test("the order is explicitly passed in both cases") {
    getTrades(order = ResultOrder.DescendingOrder)
    captureRequest().params should contain("sort", "-1")
    getTrades(order = ResultOrder.AscendingOrder)
    captureRequest().params should contain("sort", "1")
  }

  test("adding start or end timestamps sets the respective parameters on the request (-1 for exclusive end)") {
    getTrades(from = Some(milli(123)), until = Some(milli(234)))
    captureRequest().params should contain allOf(("start", "123"), ("end", "233"))
  }

  test("trades are converted with the given converter before they are returned") {
    val f = getTrades()
    fakeConversion(json(123), trade(1), trade(2), trade(3))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Success(Seq(trade(1), trade(2), trade(3)))
  }

  test("trades are returned in the given order regardless of ids") {
    val f = getTrades()
    fakeConversion(json(123), trade(1), trade(3), trade(2))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Success(Seq(trade(1), trade(3), trade(2)))
  }

  test("an error is returned when the json cannot be parsed") {
    val f = getTrades()
    fakeConversionFailure(ex("ouch"))
    baseService.completeNext(json(123))
    f.value.get shouldEqual Failure(BitfinexApiError.failedJsonConversion(json(123), ex("ouch")))
  }

  test("api errors are just forwarded") {
    val f = getTrades()
    baseService.failNext(ex("fail!"))
    f.value.get shouldEqual Failure(ex("fail!"))
  }

}
