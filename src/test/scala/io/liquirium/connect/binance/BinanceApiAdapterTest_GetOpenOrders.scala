package io.liquirium.connect.binance

import io.liquirium.connect.binance.helpers.BinanceTestHelpers.{order => binanceOrder}
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.connect.binance.BinanceRestApi.OpenOrdersRequest
import io.liquirium.core.{Order, TradingPair}

import scala.util.Failure

class BinanceApiAdapterTest_GetOpenOrders extends BinanceApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureOrderRequest(): OpenOrdersRequest = {
    val adapterCaptor = argumentCaptor[OpenOrdersRequest]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeOrderConversion(binanceOrder: BinanceOrder, genericOrder: Order) =
    fakeModelConverter.convertOrder(binanceOrder) returns genericOrder

  private def fakeOrderConversionFailure(binanceOrder: BinanceOrder, t: Throwable) =
    fakeModelConverter.convertOrder(binanceOrder) throws t

  private def getOpenOrders(
    tradingPair: TradingPair = defaultPair,
  ) = apiAdapter.getOpenOrders(tradingPair)

  private def replyWith(orders: BinanceOrder*): Unit = fakeRestApi.completeNext(orders)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("it issues a request for orders with the given converted trading pair") {
    fakeTradingPairToSymbolConversion(pair(123), "BTC-USD")
    getOpenOrders(tradingPair = pair(123))
    captureOrderRequest() should matchPattern {
      case oor: OpenOrdersRequest if oor.symbol.contains("BTC-USD") =>
    }
  }

  test("the returned orders are converted and returned in a set") {
    fakeOrderConversion(binanceOrder("1"), order("A"))
    fakeOrderConversion(binanceOrder("2"), order("B"))
    val f = getOpenOrders()
    replyWith(
      binanceOrder("1"),
      binanceOrder("2"),
    )
    f.value.get.get shouldEqual Set(
      order("A"),
      order("B"),
    )
  }

  test("a failure is returned when the model conversion fails for an order") {
    fakeOrderConversion(binanceOrder("1"), order(1))
    fakeOrderConversionFailure(binanceOrder("2"), ex(123))
    val f = getOpenOrders()
    replyWith(
      binanceOrder("1"),
      binanceOrder("2"),
    )
    f.value.get shouldEqual Failure(ex(123))
  }

  test("errors are passed along") {
    val f = getOpenOrders()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
