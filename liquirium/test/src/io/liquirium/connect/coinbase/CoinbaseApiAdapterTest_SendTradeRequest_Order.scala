package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseApiRequest.CreateOrder
import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{coinbaseCreateOrderResponseFailure, coinbaseCreateOrderResponseSuccess}
import io.liquirium.core.OrderModifier.PostOnly
import io.liquirium.core._
import io.liquirium.core.helpers.CoreHelpers.{dec, ex}
import io.liquirium.core.helpers.MarketHelpers.{pair, market => m}
import io.liquirium.core.helpers.OrderHelpers.order

import scala.util.Failure

class CoinbaseApiAdapterTest_SendTradeRequest_Order extends CoinbaseApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToProductIdConversion(defaultPair, "DEFAULT")

  private def captureOrderRequest(): CreateOrder = {
    val adapterCaptor = argumentCaptor[CreateOrder]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def sendTradeRequest(
    market: Market = m("m"),
    quantity: BigDecimal = dec(1),
    price: BigDecimal = dec(1),
    modifiers: Set[OrderModifier] = Set(),
  ) = apiAdapter.sendTradeRequest(
    OrderRequest(
      market = market,
      quantity = quantity,
      price = price,
      modifiers = modifiers
    ))

  private def replyWith(response: CoinbaseCreateOrderResponse): Unit = fakeRestApi.completeNext(response)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("it issues a request with side buy if the order request quantity was positive") {
    sendTradeRequest(quantity = dec(1))
    captureOrderRequest() should matchPattern {
      case or: CreateOrder if or.side == Side.Buy =>
    }
  }

  test("it issues a request with side sell if the order request quantity was negative") {
    sendTradeRequest(quantity = dec(-1))
    captureOrderRequest() should matchPattern {
      case or: CreateOrder if or.side == Side.Sell =>
    }
  }

  test("it issues a request with the given converted trading pair") {
    fakeTradingPairToProductIdConversion(pair(123), "BTC-USD")
    sendTradeRequest(market = m("m", pair(123)))
    captureOrderRequest() should matchPattern {
      case co: CreateOrder if co.productId == "BTC-USD" =>
    }
  }

  test("it issues a request with a random client order id") {
    sendTradeRequest()
    captureOrderRequest() should matchPattern {
      case or: CreateOrder if or.clientOrderId.nonEmpty =>
    }
  }

  test("it issues a request with the order request's quantity as base size") {
    sendTradeRequest(quantity = dec("1.23"))
    captureOrderRequest() should matchPattern {
      case or: CreateOrder if or.baseSize == 1.23 =>
    }
  }

  test("it issues a request with the order request's price as limit price") {
    sendTradeRequest(price = dec("1.23"))
    captureOrderRequest() should matchPattern {
      case or: CreateOrder if or.limitPrice == 1.23 =>
    }
  }

  test("it issues a request with post only if the request held the modifier") {
    sendTradeRequest(modifiers = Set(PostOnly))
    captureOrderRequest() should matchPattern {
      case or: CreateOrder if or.postOnly =>
    }
  }

  test("it issues a request without post only if the request didn't held the modifier") {
    sendTradeRequest(modifiers = Set())
    captureOrderRequest() should matchPattern {
      case or: CreateOrder if !or.postOnly =>
    }
  }

  test("errors are passed along") {
    val f = sendTradeRequest()
    failWith(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

  test("a order request confirmation with order and without immediate trades is returned" +
    "in case of create order response success") {
    val market = m("M")
    val f = sendTradeRequest(
      market = market,
      quantity = dec(123),
      price = dec(456)
    )
    replyWith(response = coinbaseCreateOrderResponseSuccess("id", "clientId"))
    val o = order("id", dec(123), dec(123), dec(456), market)
    f.value.get.get.asInstanceOf[OrderRequestConfirmation].order shouldEqual Some(o)
    f.value.get.get.asInstanceOf[OrderRequestConfirmation].immediateTrades shouldEqual Seq()
  }

  test("an exception is thrown in case of a create order response failure") {
    val f = sendTradeRequest()
    replyWith(coinbaseCreateOrderResponseFailure(error = "e"))
    f.value.get.isFailure shouldBe true
  }

}
