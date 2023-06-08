package io.liquirium.bot.helpers

import io.liquirium.bot.BotInput.CompletedOperationRequest
import io.liquirium.bot.OperationRequestMessage
import io.liquirium.core.TradeRequestResponseMessage.{NoSuchOpenOrderCancelFailure, PostOnlyOrderRequestFailed}
import io.liquirium.core.helpers.CoreHelpers.{dec, ex, sec}
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.helpers.TradeHelpers.trade
import io.liquirium.core._
import io.liquirium.core.helpers.MarketHelpers
import io.liquirium.util.AbsoluteQuantity
import io.liquirium.{bot, core}

import java.time.Instant

object OperationRequestHelpers {

  private val defaultMarket = MarketHelpers.m(0)

  def cancelRequest(oid: String, market: Market = defaultMarket): CancelRequest = CancelRequest(market, oid)

  def cancelRequest(n: Int): CancelRequest = cancelRequest(n.toString)

  def cancelRequest(m: Market, n: Int): CancelRequest = cancelRequest(n.toString, market = m)

  def cancelRequest(o: Order): CancelRequest = CancelRequest(o.market, o.id)

  def orderRequest(
    market: Market = defaultMarket,
    quantity: BigDecimal = BigDecimal(1),
    price: BigDecimal = BigDecimal(1),
    modifiers: Set[OrderModifier] = Set(),
  ): OrderRequest =
    OrderRequest(market, quantity, price = price, modifiers)

  def orderRequest(n: Int): OrderRequest = orderRequest(quantity = dec(n))

  def orderRequest(m: Market, n: Int): OrderRequest = orderRequest(quantity = dec(n), market = m)

  def id(n: Int): OperationRequestId = operationRequestId(n)

  def operationRequestId(n: Int): OperationRequestId = CompoundOperationRequestId(BotId("trader" + n.toString), n)

  def orderRequestMessage(n: Int): OperationRequestMessage = bot.OperationRequestMessage(id(n), orderRequest(n))

  def orderRequestMessage(
    id: OperationRequestId = this.id(0),
    market: Market = defaultMarket,
    quantity: BigDecimal = dec(1),
    price: BigDecimal = dec(1),
    modifiers: Set[OrderModifier] = Set(),
  ): OperationRequestMessage =
    bot.OperationRequestMessage(id, orderRequest(market, quantity, price = price, modifiers))

  def cancelRequestMessage(id: OperationRequestId, market: Market = defaultMarket, orderId: String = "")
  : OperationRequestMessage = bot.OperationRequestMessage(id, CancelRequest(market, orderId))

  def cancelRequestMessage(id: OperationRequestId, request: CancelRequest) : OperationRequestMessage =
    bot.OperationRequestMessage(id, request)

  def cancelRequestMessage(n: Int): OperationRequestMessage = cancelRequestMessage(id(n), cancelRequest(n))

  @deprecated(message = "use operationRequest instead")
  def request(n: Int): OperationRequest =
    if (n % 2 == 0) orderRequest(n)
    else cancelRequest(n)

  def operationRequest(n: Int): OperationRequest =
    if (n % 2 == 0) orderRequest(n)
    else cancelRequest(n)

  @deprecated(message = "use operationRequest instead")
  def request(m: Market, n: Int): OperationRequest =
    if (n % 2 == 0) orderRequest(m, n)
    else cancelRequest(m, n)

  def operationRequest(m: Market, n: Int): OperationRequest =
    if (n % 2 == 0) orderRequest(m, n)
    else cancelRequest(m, n)

  def operationRequestMessage(n: Int): OperationRequestMessage =
    if (n % 2 == 0) orderRequestMessage(n)
    else cancelRequestMessage(n)

  def orderConfirmation(n: Int): OrderRequestConfirmation = OrderRequestConfirmation(Some(order(n)), Seq(trade(n)))

  def orderConfirmation(o: Order, trades: Seq[Trade] = Seq()): OrderRequestConfirmation =
    OrderRequestConfirmation(Some(o), trades)

  def orderConfirmation(trades: Seq[Trade]): OrderRequestConfirmation = OrderRequestConfirmation(None, trades)

  def cancelConfirmation: CancelRequestConfirmation = CancelRequestConfirmation(None)

  def cancelConfirmation(rest: BigDecimal): CancelRequestConfirmation =
    CancelRequestConfirmation(Some(AbsoluteQuantity(rest)))

  def requestMessage(id: OperationRequestId, n: Int): OperationRequestMessage =
    bot.OperationRequestMessage(id, operationRequest(n))

  def operationRequestMessage[TR <: OperationRequest](id: OperationRequestId, request: TR): OperationRequestMessage =
    OperationRequestMessage(id, request)

  def requestConfirmation(n: Int): OperationRequestSuccessResponse[_ <: OperationRequest] =
    if (n % 2 == 0) OrderRequestConfirmation(None, Seq())
    else CancelRequestConfirmation(Some(AbsoluteQuantity(n)))

  def successResponseMessage(n: Int): TradeRequestResponseMessage =
    TradeRequestSuccessResponseMessage(id(n), requestConfirmation(n))

  def successResponseMessage(requestMessage: OperationRequestMessage)
  : TradeRequestSuccessResponseMessage[_ <: OperationRequest] =
    core.TradeRequestSuccessResponseMessage(requestMessage.id, successResponse(requestMessage, 0))

  def successResponse(message: OperationRequestMessage, n: Int): OperationRequestSuccessResponse[_ <: OperationRequest] =
    message.request match {
      case _: OrderRequest => orderConfirmation(order(n))
      case _: CancelRequest => CancelRequestConfirmation(Some(AbsoluteQuantity(dec(n))))
    }

  def failureResponseMessage(requestMessage: OperationRequestMessage, t: Throwable): TradeRequestFailureMessage =
    TradeRequestFailureMessage(requestMessage.id, t)

  def softFailureResponseMessage(requestMessage: OperationRequestMessage): TradeRequestFailureMessage =
    requestMessage.request match {
      case CancelRequest(_, orderId) => failureResponseMessage(requestMessage, NoSuchOpenOrderCancelFailure(orderId))
      case _ => failureResponseMessage(requestMessage, PostOnlyOrderRequestFailed)
    }

  def successfulRequestWithTime(time: Instant, requestMessage: OperationRequestMessage): CompletedOperationRequest =
    CompletedOperationRequest(time, requestMessage, Right(successResponse(requestMessage, 0)))

  def successfulRequestWithTime(
    time: Instant,
    requestMessage: OperationRequestMessage,
    response: OperationRequestSuccessResponse[_ <: OperationRequest],
  ): CompletedOperationRequest = CompletedOperationRequest(time, requestMessage, Right(response))

  def failure(n: Int, ex: Throwable): TradeRequestFailureMessage = TradeRequestFailureMessage(id(n), ex)

  def failedRequestWithTime(time: Instant, requestMessage: OperationRequestMessage, t: Throwable): CompletedOperationRequest =
    CompletedOperationRequest(time, requestMessage, Left(t))

  def completedOperationRequest(n: Int): CompletedOperationRequest =
    if (n % 2 == 1) successfulRequestWithTime(sec(n), operationRequestMessage(n))
    else failedRequestWithTime(sec(n), operationRequestMessage(n), ex(n))

}
