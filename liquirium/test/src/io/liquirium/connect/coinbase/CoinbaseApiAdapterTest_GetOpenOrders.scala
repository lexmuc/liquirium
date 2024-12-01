package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.GetOpenOrders
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.coinbaseOrder
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.{Order, TradingPair}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.matchPattern

import scala.util.Failure

class CoinbaseApiAdapterTest_GetOpenOrders extends CoinbaseApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToProductIdConversion(defaultPair, "DEFAULT")

  private def captureOrderRequest(): GetOpenOrders = {
    val adapterCaptor = argumentCaptor[GetOpenOrders]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def fakeOrderConversion(coinbaseOrder: CoinbaseOrder, genericOrder: Order) =
    fakeModelConverter.convertOrder(coinbaseOrder) returns genericOrder

  private def getOpenOrders(
    tradingPair: TradingPair = defaultPair,
  ) = apiAdapter.getOpenOrders(tradingPair)

  private def replyWith(orders: CoinbaseOrder*): Unit = fakeRestApi.completeNext(orders)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("it issues a request for orders with the given converted trading pair") {
    fakeTradingPairToProductIdConversion(pair(123), "BTC-USD")
    getOpenOrders(tradingPair = pair(123))
    captureOrderRequest() should matchPattern {
      case oo: GetOpenOrders if oo.productId.contains("BTC-USD") =>
    }
  }

  test("the limit is set to the configured maximum") {
    maxOrderBatchSize = 234
    getOpenOrders()
    captureOrderRequest() should matchPattern {
      case oo: GetOpenOrders if oo.limit == 234 =>
    }
  }

  test("the returned orders are converted and returned in a set") {
    fakeOrderConversion(coinbaseOrder("1"), order("A"))
    fakeOrderConversion(coinbaseOrder("2"), order("B"))
    val f = getOpenOrders()
    replyWith(
      coinbaseOrder("1"),
      coinbaseOrder("2"),
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
