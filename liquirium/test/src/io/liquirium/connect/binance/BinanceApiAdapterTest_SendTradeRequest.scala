package io.liquirium.connect.binance

import io.liquirium.bot.helpers.OperationRequestHelpers.{cancelRequest, orderRequest}
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.{order => binanceOrder}
import io.liquirium.core.OrderModifier.PostOnly
import io.liquirium.core.helpers.CoreHelpers.{dec, ex}
import io.liquirium.core.helpers.MarketHelpers
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.{CancelRequestConfirmation, OperationRequest, Order, OrderRequestConfirmation, Side}
import io.liquirium.util.AbsoluteQuantity
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.util.Failure

class BinanceApiAdapterTest_SendTradeRequest extends BinanceApiAdapterTest {

  private val defaultPair = pair(1)

  fakeTradingPairToSymbolConversion(defaultPair, "DEFAULT")

  private def captureNewOrderRequest(): BinanceRestApi.NewOrderRequest = {
    val adapterCaptor = argumentCaptor[BinanceRestApi.NewOrderRequest]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def captureCancelOrderRequest(): BinanceRestApi.CancelOrderRequest = {
    val adapterCaptor = argumentCaptor[BinanceRestApi.CancelOrderRequest]
    fakeRestApi.verify.sendRequest(adapterCaptor.capture())
    adapterCaptor.getValue
  }

  private def sendOperationRequest(request: OperationRequest) = apiAdapter.sendTradeRequest(request)

  private def failWith(t: Throwable): Unit = fakeRestApi.failNext(t)

  private def fakeOrderConversion(binanceOrder: BinanceOrder, genericOrder: Order) =
    fakeModelConverter.convertOrder(binanceOrder) returns genericOrder

  private def fakeOrderConversionFailure(binanceOrder: BinanceOrder, t: Throwable) =
    fakeModelConverter.convertOrder(binanceOrder) throws t

  test("an order request yields a binance new order request with correct symbol and price") {
    fakeTradingPairToSymbolConversion(pair(123), "BTCUSD")
    sendOperationRequest(orderRequest(
      market = MarketHelpers.market("...", pair(123)),
      price = dec(234),
    ))
    val nor = captureNewOrderRequest()
    nor.symbol shouldEqual "BTCUSD"
    nor.price shouldEqual dec(234)
  }

  test("the side depends on the sign of the quantity and the resulting quantity is always positive") {
    sendOperationRequest(orderRequest(
      quantity = dec(123),
    ))
    val buyRequest = captureNewOrderRequest()
    buyRequest.side shouldEqual Side.Buy
    buyRequest.quantity shouldEqual dec(123)

    fakeRestApi.reset()

    sendOperationRequest(orderRequest(
      quantity = dec(-123),
    ))
    val sellRequest = captureNewOrderRequest()
    sellRequest.side shouldEqual Side.Sell
    sellRequest.quantity shouldEqual dec(123)
  }

  test("the type is LIMIT unless postOnly is set, then it is LIMIT_MAKER") {
    sendOperationRequest(orderRequest(
      modifiers = Set(),
    ))
    captureNewOrderRequest().orderType shouldEqual BinanceOrderType.LIMIT

    fakeRestApi.reset()

    sendOperationRequest(orderRequest(
      modifiers = Set(PostOnly),
    ))
    captureNewOrderRequest().orderType shouldEqual BinanceOrderType.LIMIT_MAKER
  }

  test("the order request response is a confirmation with the converted order and without trades") {
    fakeOrderConversion(binanceOrder("A"), order(1))
    val f = sendOperationRequest(orderRequest())
    fakeRestApi.completeNext(binanceOrder("A"))
    f.value.get.get shouldEqual OrderRequestConfirmation(Some(order(1)), Seq())
  }

  test("a cancel request is yields a cancel with the respective order id and the converted pair/symbol") {
    fakeTradingPairToSymbolConversion(pair(123), "S123")
    sendOperationRequest(cancelRequest("O-123", MarketHelpers.market("X", pair(123))))
    val req = captureCancelOrderRequest()
    req.symbol shouldEqual "S123"
    req.orderId shouldEqual "O-123"
  }

  test("a cancel confirmation contains the given rest quantity") {
    val f = sendOperationRequest(cancelRequest(1))
    fakeRestApi.completeNext(binanceOrder("x", originalQuantity = dec(70), executedQuantity = dec(5)))
    val response = f.value.get.get
    response shouldEqual CancelRequestConfirmation(Some(AbsoluteQuantity(dec(65))))
  }

  test("conversion failures are passed along") {
    fakeOrderConversionFailure(binanceOrder("A"), ex(123))
    val f = sendOperationRequest(orderRequest())
    fakeRestApi.completeNext(binanceOrder("A"))
    f.value.get shouldEqual Failure(ex(123))
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
