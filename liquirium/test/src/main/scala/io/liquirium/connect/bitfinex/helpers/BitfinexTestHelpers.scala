package io.liquirium.connect.bitfinex.helpers

import io.liquirium.connect.bitfinex
import io.liquirium.connect.bitfinex.BitfinexInMessage._
import io.liquirium.connect.bitfinex.BitfinexOrder.{OrderStatus, OrderType}
import io.liquirium.connect.bitfinex.BitfinexOutMessage._
import io.liquirium.connect.bitfinex._
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import play.api.libs.json.JsNumber

import java.time.Instant

object BitfinexTestHelpers {

  def order
  (
    id: Long = 0,
    clientOrderId: Long = 0,
    symbol: String = "tASDFJK",
    creationTimestamp: Instant = Instant.ofEpochMilli(0),
    updateTimestamp: Instant = Instant.ofEpochMilli(0),
    amount: BigDecimal = BigDecimal("1"),
    originalAmount: BigDecimal = BigDecimal("1"),
    `type`: OrderType = OrderType.Limit,
    status: OrderStatus = OrderStatus.Active,
    price: BigDecimal = BigDecimal("1")
  ): BitfinexOrder =
    BitfinexOrder(
      id = id,
      clientOrderId = clientOrderId,
      symbol = symbol,
      creationTimestamp = creationTimestamp,
      updateTimestamp = updateTimestamp,
      amount = amount,
      originalAmount = originalAmount,
      `type` = `type`,
      status = status,
      price = price
    )

  def newOrder(
    id: Long = 0,
    clientOrderId: Long = 0,
    symbol: String = "tASDFJK",
    timestamp: Instant = Instant.ofEpochMilli(0),
    amount: BigDecimal = BigDecimal("1"),
    `type`: OrderType = OrderType.Limit,
    price: BigDecimal = BigDecimal("1"),
  ): BitfinexOrder = order(
    id = id,
    clientOrderId = clientOrderId,
    symbol = symbol,
    creationTimestamp = timestamp,
    updateTimestamp = timestamp,
    amount = amount,
    originalAmount = amount,
    `type` = `type`,
    status = OrderStatus.Active,
    price = price
  )

  def partialOrder(
    id: Long = 0,
    clientOrderId: Long = 0,
    symbol: String = "tASDFJK",
    timestamps: (Instant, Instant) = (Instant.ofEpochMilli(0), Instant.ofEpochMilli(0)),
    amounts: (BigDecimal, BigDecimal) = (BigDecimal("0.5"), BigDecimal("1")),
    `type`: OrderType = OrderType.Limit,
    price: BigDecimal = BigDecimal("1"),
  ): BitfinexOrder = order(
    id = id,
    clientOrderId = clientOrderId,
    symbol = symbol,
    creationTimestamp = timestamps._2,
    updateTimestamp = timestamps._1,
    amount = amounts._1,
    originalAmount = amounts._2,
    `type` = `type`,
    status = OrderStatus.PartiallyFilled,
    price = price
  )

  def orders(n: Int): Set[BitfinexOrder] = Set(order(n), order(n + 1))

  def trade
  (
    id: Long = 0,
    symbol: String = "tABCDEF",
    timestamp: Instant = Instant.ofEpochMilli(0),
    orderId: Long = 0,
    amount: BigDecimal = BigDecimal("0"),
    price: BigDecimal = BigDecimal("1"),
    fee: BigDecimal = BigDecimal("0"),
    feeCurrency: String = ""
  ): BitfinexTrade =
    bitfinex.BitfinexTrade(
      id = id,
      symbol = symbol,
      timestamp = timestamp,
      orderId = orderId,
      amount = amount,
      price = price,
      fee = fee,
      feeCurrency = feeCurrency
    )

  def tradeWithTimestamp(id: Long, timestamp: Instant): BitfinexTrade = trade(id, timestamp = timestamp)

  def candle
  (
    timestamp: Instant = Instant.ofEpochMilli(0),
    open: BigDecimal = BigDecimal(1),
    close: BigDecimal = BigDecimal(1),
    high: BigDecimal = BigDecimal(1),
    low: BigDecimal = BigDecimal(1),
    volume: BigDecimal = BigDecimal(1)
  ): BitfinexCandle =
    bitfinex.BitfinexCandle(
      timestamp = timestamp,
      open = open,
      close = close,
      high = high,
      low = low,
      volume = volume
    )

  def candle(n: Int): BitfinexCandle = candle(timestamp = sec(n))

  def apiError(s: String): ExplicitBitfinexApiError = ExplicitBitfinexApiError(s)

  def cancelOrder(n: Int): CancelOrderMessage = CancelOrderMessage(n)

  def placeOrder(n: Int): PlaceOrderMessage = PlaceOrderMessage(n, n.toString, OrderType.Limit, n, n, Set())

  def tradeRequest(n: Int): TradeRequestMessage = if (n % 2 == 0) cancelOrder(n) else placeOrder(n)

  def errorMessage(n: Int): ErrorMessage = ErrorMessage(n.toString, n)

  def newTradeMessage(n: Int): NewTradeMessage = NewTradeMessage(trade(n))

  def orderStateMessage(n: Int): OrderStateMessage = OrderStateMessage(Seq(order(n)))

  def orderCanceledMessage(n: Int): OrderCancelMessage = OrderCancelMessage(order(n))

  def newOrderMessage(n: Int): NewOrderMessage = NewOrderMessage(order(n))

  def irrelevantMessage(n: Int): IrrelevantMessage = IrrelevantMessage(JsNumber(n))

  def tradeHistory(since: Instant, tt: BitfinexTrade*): TradeHistory = TradeHistory(tt, since)

  def pairInfo(n: Int): BitfinexPairInfo =
    BitfinexPairInfo(
      pair = "PAIR" + n,
      minOrderSize = dec(n),
      maxOrderSize = dec(n + 1),
    )

}
