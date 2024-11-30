package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.{order => bitfinexOrder}
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.{Order, TradingPair}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.matchPattern

import scala.util.Failure

class BitfinexApiAdapterTest_GetOpenOrders extends BitfinexApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureOrderRequest(): BitfinexRestApi.GetOpenOrders = {
    val adapterCaptor = argumentCaptor[BitfinexRestApi.GetOpenOrders]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeOrderConversion(bitfinexOrder: BitfinexOrder, genericOrder: Order) =
    fakeModelConverter.convertOrder(bitfinexOrder) returns genericOrder

  private def fakeOrderConversionFailure(bitfinexOrder: BitfinexOrder, t: Throwable) =
    fakeModelConverter.convertOrder(bitfinexOrder) throws t

  private def getOpenOrders(
    tradingPair: TradingPair = defaultPair,
  ) = apiAdapter.getOpenOrders(tradingPair)

  private def replyWith(orders: BitfinexOrder*): Unit = fakeRestApi.completeNext(orders)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("it issues a request for orders with the given converted trading pair") {
    fakeTradingPairToSymbolConversion(pair(123), "BTC-USD")
    getOpenOrders(tradingPair = pair(123))
    captureOrderRequest() should matchPattern {
      case goh: BitfinexRestApi.GetOpenOrders if goh.symbol.contains("BTC-USD") =>
    }
  }

  test("the returned orders are converted and returned in a set") {
    fakeOrderConversion(bitfinexOrder(1), order("A"))
    fakeOrderConversion(bitfinexOrder(2), order("B"))
    val f = getOpenOrders()
    replyWith(
      bitfinexOrder(1),
      bitfinexOrder(2),
    )
    f.value.get.get shouldEqual Set(
      order("A"),
      order("B"),
    )
  }

  test("a failure is returned when the model conversion fails for an order") {
    fakeOrderConversion(bitfinexOrder(1), order(1))
    fakeOrderConversionFailure(bitfinexOrder(2), ex(123))
    val f = getOpenOrders()
    replyWith(
      bitfinexOrder(1),
      bitfinexOrder(2),
    )
    f.value.get shouldEqual Failure(ex(123))
  }

  test("errors are passed along") {
    val f = getOpenOrders()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
