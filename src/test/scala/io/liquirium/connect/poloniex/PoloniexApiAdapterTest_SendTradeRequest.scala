package io.liquirium.connect.poloniex

import io.liquirium.bot.helpers.OperationRequestHelpers.{cancelRequest, orderRequest}
import io.liquirium.connect.poloniex.PoloniexApiRequest.{CancelOrderById, CreateOrder}
import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.poloniexCreateOrderResponse
import io.liquirium.core.OrderModifier.PostOnly
import io.liquirium.core.helpers.CoreHelpers.{dec, ex}
import io.liquirium.core.helpers.MarketHelpers
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.{OrderRequestConfirmation, Side, OperationRequest}

import scala.util.Failure

class PoloniexApiAdapterTest_SendTradeRequest extends PoloniexApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureNewOrderRequest(): CreateOrder = {
    val adapterCaptor = argumentCaptor[CreateOrder]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def captureCancelOrderRequest(): CancelOrderById = {
    val adapterCaptor = argumentCaptor[CancelOrderById]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def sendOperationRequest(request: OperationRequest) = apiAdapter.sendTradeRequest(request)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)


  test("an order request yields a poloniex new order request with correct symbol and price") {
    fakeTradingPairToSymbolConversion(pair(123), "BTCUSD")
    sendOperationRequest(orderRequest(
      market = MarketHelpers.market("...", pair(123)),
      price = dec(234),
    ))
    val nor = captureNewOrderRequest()
    nor.symbol shouldEqual "BTCUSD"
    nor.price shouldEqual Some(dec(234))
  }

  test("the side depends on the sign of the quantity and the resulting quantity is always positive") {
    sendOperationRequest(orderRequest(
      quantity = dec(123),
    ))
    val buyRequest = captureNewOrderRequest()
    buyRequest.side shouldEqual Side.Buy
    buyRequest.quantity shouldEqual Some(dec(123))

    fakeRestApi.reset()

    sendOperationRequest(orderRequest(
      quantity = dec(-123),
    ))
    val sellRequest = captureNewOrderRequest()
    sellRequest.side shouldEqual Side.Sell
    sellRequest.quantity shouldEqual Some(dec(123))
  }

  test("the type is LIMIT unless postOnly is set, then it is LIMIT_MAKER") {
    sendOperationRequest(orderRequest(
      modifiers = Set(),
    ))
    captureNewOrderRequest().`type` shouldEqual Some(PoloniexOrderType.LIMIT)

    fakeRestApi.reset()

    sendOperationRequest(orderRequest(
      modifiers = Set(PostOnly),
    ))
    captureNewOrderRequest().`type` shouldEqual Some(PoloniexOrderType.LIMIT_MAKER)
  }

  test("the order request response is a confirmation with order and without trades") {
    val market = MarketHelpers.market("ABC", pair(123))
    val f = sendOperationRequest(orderRequest(
      market = market,
      quantity = dec(123),
      price = dec(789),
    ))
    fakeRestApi.completeNext(poloniexCreateOrderResponse(id = "xxx"))
    val responseOrder = order("xxx", dec(123), dec(123), dec(789), market)
    f.value.get.get shouldEqual OrderRequestConfirmation(Some(responseOrder), Seq())
  }

  test("a cancel request yields a cancel with the respective order id") {
    sendOperationRequest(cancelRequest("O-123"))
    val req = captureCancelOrderRequest()
    req.orderId shouldEqual "O-123"
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
