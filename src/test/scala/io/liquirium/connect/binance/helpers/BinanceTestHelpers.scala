package io.liquirium.connect.binance.helpers

import io.liquirium.connect.binance
import io.liquirium.connect.binance.{BinanceCandle, BinanceExecutionReport, BinanceExecutionType, BinanceOrder, BinanceTrade}
import io.liquirium.core.Side
import io.liquirium.core.helper.CoreHelpers.{dec, sec}

import java.time.Instant

object BinanceTestHelpers {

  def candle
  (
    openTime: Instant = Instant.ofEpochMilli(0),
    open: BigDecimal = BigDecimal(1),
    close: BigDecimal = BigDecimal(1),
    high: BigDecimal = BigDecimal(1),
    low: BigDecimal = BigDecimal(1),
    quoteAssetVolume: BigDecimal = BigDecimal(1),
    closeTime: Instant = Instant.ofEpochMilli(1)
  ): BinanceCandle =
    binance.BinanceCandle(
      openTime = openTime,
      open = open,
      close = close,
      high = high,
      low = low,
      quoteAssetVolume = quoteAssetVolume,
      closeTime = closeTime
    )

  def candle(n: Int): BinanceCandle = candle(openTime = sec(n))

  def order
  (
    id: String = "0",
    symbol: String = "ABCDEF",
    clientOrderId: String = "",
    price: BigDecimal = dec(1),
    originalQuantity: BigDecimal = dec(1),
    executedQuantity: BigDecimal = dec(0),
    `type`: String = "LIMIT",
    side: Side = Side.Buy
  ): BinanceOrder =
    BinanceOrder(
      id = id,
      symbol = symbol,
      clientOrderId = clientOrderId,
      price = price,
      originalQuantity = originalQuantity,
      executedQuantity = executedQuantity,
      `type` = `type`,
      side = side
    )

  def trade
  (
    id: String = "0",
    symbol: String = "AAABBB",
    orderId: String = "0",
    price: BigDecimal = dec(1),
    quantity: BigDecimal = dec(1),
    commission: BigDecimal = dec(0),
    commissionAsset: String = "CCC",
    time: Instant = Instant.ofEpochMilli(0),
    isBuyer: Boolean = false,
  ): BinanceTrade =
    BinanceTrade(
      id = id,
      symbol = symbol,
      orderId = orderId,
      price = price,
      quantity = quantity,
      commission = commission,
      commissionAsset = commissionAsset,
      time = time,
      isBuyer = isBuyer,
    )

  def executionReport
  (
    eventTime: Long = 0,
    symbol: String = "AAABBB",
    clientOrderId: String = "",
    side: Side = Side.Buy,
    orderType: String = "",
    orderQuantity: BigDecimal = dec(1),
    orderPrice: BigDecimal = dec(1),
    currentExecutionType: BinanceExecutionType = BinanceExecutionType.NEW,
    orderId: Long = 1,
    lastExecutedQuantity: BigDecimal = dec(0),
    lastExecutedPrice: BigDecimal = dec(0),
    commissionAmount: BigDecimal = dec(0),
    commissionAsset: Option[String] = None,
    transactionTime: Long = 0,
    tradeId: Option[Long] = None,
  ): BinanceExecutionReport = BinanceExecutionReport(
    eventTime = eventTime,
    symbol = symbol,
    clientOrderId = clientOrderId,
    side = side,
    orderType = orderType,
    orderQuantity = orderQuantity,
    orderPrice = orderPrice,
    currentExecutionType = currentExecutionType,
    orderId = orderId,
    lastExecutedQuantity = lastExecutedQuantity,
    lastExecutedPrice = lastExecutedPrice,
    commissionAmount = commissionAmount,
    commissionAsset = commissionAsset,
    transactionTime = transactionTime,
    tradeId = tradeId,
  )

}
