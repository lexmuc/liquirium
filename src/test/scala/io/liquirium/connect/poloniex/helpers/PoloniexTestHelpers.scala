package io.liquirium.connect.poloniex.helpers

import io.liquirium.connect.poloniex._
import io.liquirium.core.Side
import io.liquirium.core.helpers.CoreHelpers.dec

import java.time.{Duration, Instant}

object PoloniexTestHelpers {

  val typicalCurrencyPair = "BTC_USDT"

  def poloniexCandle(
    low: BigDecimal = BigDecimal(1),
    high: BigDecimal = BigDecimal(1),
    open: BigDecimal = BigDecimal(1),
    close: BigDecimal = BigDecimal(1),
    amount: BigDecimal = BigDecimal(1),
    quantity: BigDecimal = BigDecimal(1),
    buyTakerAmount: BigDecimal = BigDecimal(1),
    buyTakerQuantity: BigDecimal = BigDecimal(1),
    tradeCount: Int = 1,
    ts: Instant = Instant.ofEpochMilli(0),
    weightedAverage: BigDecimal = BigDecimal(1),
    interval: PoloniexCandleLength = PoloniexCandleLength.forDuration(Duration.ofMinutes(1)),
    startTime: Instant = Instant.ofEpochMilli(0),
    closeTime: Instant = Instant.ofEpochMilli(60000),
  ): PoloniexCandle = PoloniexCandle(
    low = low,
    high = high,
    open = open,
    close = close,
    amount = amount,
    quantity = quantity,
    buyTakerAmount = buyTakerAmount,
    buyTakerQuantity = buyTakerQuantity,
    tradeCount = tradeCount,
    ts = ts,
    weightedAverage = weightedAverage,
    interval = interval,
    startTime = startTime,
    closeTime = closeTime,
  )

  def poloniexCandle(n: Int): PoloniexCandle = poloniexCandle(startTime = Instant.ofEpochSecond(n))

  def poloniexCandle(d: Duration): PoloniexCandle = poloniexCandle(interval = PoloniexCandleLength.forDuration(d))


  def poloniexTrade(
    id: String = "abc",
    symbol: String = "LINK_USDT",
    accountType: String = "SPOT",
    orderId: String = "123",
    side: Side = Side.Sell,
    `type`: String = "MARKET",
    matchRole: String = "TAKER",
    createTime: Instant = Instant.ofEpochMilli(1000),
    price: BigDecimal = BigDecimal("7.441233140655106"),
    quantity: BigDecimal = BigDecimal("1.557"),
    amount: BigDecimal = BigDecimal("11.586"),
    feeCurrency: String = "USDT",
    feeAmount: BigDecimal = BigDecimal("1.751"),
    pageId: String = "123",
    clientOrderId: String = "ownId",
  ): PoloniexTrade =
    PoloniexTrade(
      id = id,
      symbol = symbol,
      accountType = accountType,
      orderId = orderId,
      side = side,
      `type` = `type`,
      matchRole = matchRole,
      createTime = createTime,
      price = price,
      quantity = quantity,
      amount = amount,
      feeCurrency = feeCurrency,
      feeAmount = feeAmount,
      pageId = pageId,
      clientOrderId = clientOrderId
    )

  def poloniexTrade(n: Int): PoloniexTrade = poloniexTrade(id = n.toString)

  def poloniexOrder(
    id: String = "12345",
    clientOrderId: String = "Try1",
    symbol: String = "TRX_USDC",
    accountType: String = "SPOT",
    side: Side = Side.Sell,
    `type`: String = "LIMIT_MAKER",
    timeInForce: String = "GTC",
    price: BigDecimal = BigDecimal("100"),
    avgPrice: BigDecimal = BigDecimal("99"),
    quantity: BigDecimal = BigDecimal("5"),
    amount: BigDecimal = BigDecimal("500"),
    filledQuantity: BigDecimal = BigDecimal("10"),
    filledAmount: BigDecimal = BigDecimal("5"),
    state: String = "FILLED",
    createTime: Instant = Instant.ofEpochMilli(1000),
    updateTime: Instant = Instant.ofEpochMilli(500),
  ): PoloniexOrder =
    PoloniexOrder(
      id = id,
      clientOrderId = clientOrderId,
      symbol = symbol,
      accountType = accountType,
      side = side,
      `type` = `type`,
      timeInForce = timeInForce,
      price = price,
      avgPrice = avgPrice,
      quantity = quantity,
      amount = amount,
      filledQuantity = filledQuantity,
      filledAmount = filledAmount,
      state = state,
      createTime = createTime,
      updateTime = updateTime,
    )

  def poloniexCreateOrderResponse(
    id: String = "abc",
    clientOrderId: String = "111"
  ): PoloniexCreateOrderResponse =
    PoloniexCreateOrderResponse(
      id = id,
      clientOrderId = clientOrderId
    )

  def poloniexCancelOrderByIdResponse(
    orderId: String = "abc",
    clientOrderId: String = "xxx",
    state: String = "PENDING_CANCEL",
    code: Int = 200,
    message: String = "cancel message",
  ): PoloniexCancelOrderByIdResponse =
    PoloniexCancelOrderByIdResponse(
      orderId = orderId,
      clientOrderId = clientOrderId,
      state = state,
      code = code,
      message = message
    )

  def symbolInfo(
    symbol: String = "ETCBTC",
    priceScale: Int = 1,
    quantityScale: Int = 1,
    minAmount: BigDecimal = dec(1),
    minQuantity: BigDecimal = dec(1),
  ): PoloniexSymbolInfo =
    PoloniexSymbolInfo(
      symbol = symbol,
      priceScale = priceScale,
      quantityScale = quantityScale,
      minAmount = minAmount,
      minQuantity = minQuantity,
    )

  def symbolInfo(n: Int): PoloniexSymbolInfo =
    symbolInfo(
      symbol = "symbol" + n,
      priceScale = n,
    )

}
