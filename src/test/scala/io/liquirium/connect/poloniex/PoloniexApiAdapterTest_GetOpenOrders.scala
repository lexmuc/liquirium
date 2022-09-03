package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.PoloniexApiRequest.GetOpenOrders
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.poloniexOrder
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.{Order, TradingPair}

import scala.util.Failure

class PoloniexApiAdapterTest_GetOpenOrders extends PoloniexApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureOrderRequest(): GetOpenOrders = {
    val adapterCaptor = argumentCaptor[GetOpenOrders]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeOrderConversion(poloniexOrder: PoloniexOrder, genericOrder: Order) =
    fakeModelConverter.convertOrder(poloniexOrder) returns genericOrder

  private def getOpenOrders(
    tradingPair: TradingPair = defaultPair,
  ) = apiAdapter.getOpenOrders(tradingPair)

  private def replyWith(orders: PoloniexOrder*): Unit = fakeRestApi.completeNext(orders)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("it issues a request for orders with the given converted trading pair") {
    fakeTradingPairToSymbolConversion(pair(123), "BTC-USD")
    getOpenOrders(tradingPair = pair(123))
    captureOrderRequest() should matchPattern {
      case oo: GetOpenOrders if oo.symbol.contains("BTC-USD") =>
    }
  }

  test("it issues a request without side, from and direction") {
    getOpenOrders()
    captureOrderRequest() should matchPattern {
      case oo: GetOpenOrders if oo.side.isEmpty =>
      case oo: GetOpenOrders if oo.from.isEmpty =>
      case oo: GetOpenOrders if oo.direction.isEmpty =>
    }
  }

  test("the limit is set to the configured maximum") {
    maxOrderBatchSize = 234
    getOpenOrders()
    captureOrderRequest() should matchPattern {
      case oo: GetOpenOrders if oo.limit.contains(234) =>
    }
  }

  test("the returned orders are converted and returned in a set") {
    fakeOrderConversion(poloniexOrder("1"), order("A"))
    fakeOrderConversion(poloniexOrder("2"), order("B"))
    val f = getOpenOrders()
    replyWith(
      poloniexOrder("1"),
      poloniexOrder("2"),
    )
    f.value.get.get shouldEqual Set(
      order("A"),
      order("B"),
    )
  }

  test("errors are passed along") {
    val f = getOpenOrders()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
