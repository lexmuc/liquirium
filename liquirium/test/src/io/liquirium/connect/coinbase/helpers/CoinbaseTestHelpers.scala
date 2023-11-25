package io.liquirium.connect.coinbase.helpers

import io.liquirium.connect.coinbase._
import io.liquirium.core.Side
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}

import java.time.Instant

object CoinbaseTestHelpers {

  val typicalCurrenyPair = "BTC-USDT"

  def coinbaseCandle(
    start: Instant = Instant.ofEpochMilli(0),
    low: BigDecimal = dec(1),
    high: BigDecimal = dec(1),
    open: BigDecimal = dec(1),
    close: BigDecimal = dec(1),
    volume: BigDecimal = dec(1),
  ): CoinbaseCandle =
    CoinbaseCandle(
      start = start,
      open = open,
      close = close,
      high = high,
      low = low,
      volume = volume,
    )

  def coinbaseCandle(n: Int): CoinbaseCandle = coinbaseCandle(start = sec(n))

  def coinbaseOrder(
    orderId: String = "abc",
    productId: String = "TRX-USDC",
    fullQuantity: BigDecimal = BigDecimal("100"),
    filledQuantity: BigDecimal = BigDecimal("10"),
    side: Side = Side.Sell,
    price: BigDecimal = BigDecimal("5"),
  ): CoinbaseOrder =
    CoinbaseOrder(
      orderId = orderId,
      productId = productId,
      fullQuantity = fullQuantity,
      filledQuantity = filledQuantity,
      side = side,
      price = price,
    )

  def coinbaseTrade(
    entryId: String = "e123",
    tradeId: String = "t456",
    orderId: String = "o789",
    tradeTime: Instant = Instant.ofEpochMilli(1000),
    tradeType: String = "FILL",
    price: BigDecimal = BigDecimal("7.441233140655106"),
    size: BigDecimal = BigDecimal("11.586"),
    commission: BigDecimal = BigDecimal("1.751"),
    productId: String = "BTC-USD",
    sequenceTimestamp: Instant = Instant.ofEpochMilli(500),
    side: Side = Side.Sell,
  ): CoinbaseTrade =
    CoinbaseTrade(
      entryId = entryId,
      tradeId = tradeId,
      orderId = orderId,
      tradeTime = tradeTime,
      tradeType = tradeType,
      price = price,
      size = size,
      commission = commission,
      productId = productId,
      sequenceTimestamp = sequenceTimestamp,
      side = side,
    )

  def coinbaseTrade(s: String): CoinbaseTrade = coinbaseTrade(tradeId = s)

  def coinbaseCreateOrderResponseSuccess(
    orderId: String = "abc",
    clientOrderId: String = "111",
  ): CoinbaseCreateOrderResponse =
    CoinbaseCreateOrderResponse.Success(
      orderId = orderId,
      clientOrderId = clientOrderId,
    )

  def coinbaseCreateOrderResponseFailure(
    error: String = "error",
    message: String = "message",
    details: String = "details",
  ): CoinbaseCreateOrderResponse =
    CoinbaseCreateOrderResponse.Failure(
      error = error,
      message = message,
      details = details,
    )

  def coinbaseCancelOrderResult(
    success: Boolean = true,
    failure_reason: String = "failure reason",
    order_id: String = "abc",
  ): CoinbaseCancelOrderResult =
    CoinbaseCancelOrderResult(
      success = success,
      failureReason = failure_reason,
      orderId = order_id,
    )

  def productInfo(
    symbol: String = "ETCBTC",
    baseIncrement: BigDecimal = dec(1),
    baseMinSize: BigDecimal = dec(1),
    baseMaxSize: BigDecimal = dec(1),
    quoteIncrement: BigDecimal = dec(1),
    quoteMinSize: BigDecimal = dec(1),
    quoteMaxSize: BigDecimal = dec(1),
  ): CoinbaseProductInfo = CoinbaseProductInfo(
    symbol = symbol,
    baseIncrement = baseIncrement,
    baseMinSize = baseMinSize,
    baseMaxSize = baseMaxSize,
    quoteIncrement = quoteIncrement,
    quoteMinSize = quoteMinSize,
    quoteMaxSize = quoteMaxSize,
  )

  def productInfo(n: Int): CoinbaseProductInfo =
    productInfo(
      symbol = "symbol" + n,
      quoteIncrement = dec(n),
    )

}
