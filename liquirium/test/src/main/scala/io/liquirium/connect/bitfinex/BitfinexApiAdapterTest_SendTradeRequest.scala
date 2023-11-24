package io.liquirium.connect.bitfinex

import io.liquirium.bot.helpers.OperationRequestHelpers.{cancelRequest, orderRequest}
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers
import io.liquirium.core.OrderModifier.PostOnly
import io.liquirium.core.helpers.CoreHelpers.{dec, ex}
import io.liquirium.core.helpers.MarketHelpers
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.{CancelRequestConfirmation, OrderRequestConfirmation, OperationRequest}
import io.liquirium.util.AbsoluteQuantity

import scala.util.Failure

class BitfinexApiAdapterTest_SendTradeRequest extends BitfinexApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureSubmitOrderRequest(): BitfinexRestApi.SubmitOrder = {
    val adapterCaptor = argumentCaptor[BitfinexRestApi.SubmitOrder]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def captureCancelOrderRequest(): BitfinexRestApi.CancelOrder = {
    val adapterCaptor = argumentCaptor[BitfinexRestApi.CancelOrder]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def sendOperationRequest(request: OperationRequest) = apiAdapter.sendTradeRequest(request)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  test("an order request yields a bitfinex submit order request with correct symbol and price") {
    fakeTradingPairToSymbolConversion(pair(123), "BTCUSD")
    sendOperationRequest(orderRequest(
      market = MarketHelpers.market("...", pair(123)),
      price = dec("123.4"),
    ))
    val sor = captureSubmitOrderRequest()
    sor.symbol shouldEqual "BTCUSD"
    sor.price shouldEqual Some(dec("123.4"))
  }

  test("an order request yields a bitfinex submit order request with always type exchange limit") {
    sendOperationRequest(orderRequest())
    val sor = captureSubmitOrderRequest()
    sor.`type` shouldEqual BitfinexOrder.OrderType.ExchangeLimit
  }

  test("an order request yields a bitfinex submit order request with correct amount") {
    sendOperationRequest(orderRequest(
      quantity = dec("123.4")
    ))
    val sor = captureSubmitOrderRequest()
    sor.amount shouldEqual dec("123.4")
  }

  test("an order request yields a bitfinex submit order request with correct flags") {
    sendOperationRequest(orderRequest(
      modifiers = Set(),
    ))
    captureSubmitOrderRequest().flags shouldEqual Set()

    fakeRestApi.reset()

    sendOperationRequest(orderRequest(
      modifiers = Set(PostOnly),
    ))
    captureSubmitOrderRequest().flags shouldEqual Set(BitfinexOrderFlag.PostOnly)
  }

  test("the order request response is a confirmation with order and without trades") {
    val market = MarketHelpers.market("ABC", pair(123))
    val f = sendOperationRequest(orderRequest(
      market = market,
    ))
    fakeRestApi.completeNext(BitfinexTestHelpers.order(id = 123, amount = dec(123), originalAmount = dec(123), price = dec(789)))
    val responseOrder = order("123", dec(123), dec(123), dec(789), market)
    f.value.get.get shouldEqual OrderRequestConfirmation(Some(responseOrder), Seq())
  }

  test("a cancel request yields a cancel order request with the respective order id") {
    sendOperationRequest(cancelRequest("123"))
    val req = captureCancelOrderRequest()
    req.id shouldEqual 123
  }

  test("the cancel request response is a cancel request confirmation with correct absolute quantity") {
    val f1 = sendOperationRequest(cancelRequest("123"))
    fakeRestApi.completeNext(BitfinexTestHelpers.order(amount = dec(123)))
    f1.value.get.get shouldEqual CancelRequestConfirmation(Some(AbsoluteQuantity(dec(123))))

    val f2 = sendOperationRequest(cancelRequest("123"))
    fakeRestApi.completeNext(BitfinexTestHelpers.order(amount = dec(-123)))
    f2.value.get.get shouldEqual CancelRequestConfirmation(Some(AbsoluteQuantity(dec(123))))
  }

  test("errors are passed along") {
    val fA = sendOperationRequest(orderRequest())
    failWith(ex(123))
    fA.value.get shouldEqual Failure(ex(123))

    fakeRestApi.reset()

    val fB = sendOperationRequest(cancelRequest(1))
    failWith(ex(123))
    fB.value.get shouldEqual Failure(ex(123))
  }
}
