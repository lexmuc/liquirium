package io.liquirium.connect.binance

import io.liquirium.core.Side

sealed trait BinanceStreamPayload {
}

case class BinanceExecutionReport(
  eventTime: Long,
  symbol: String,
  clientOrderId: String,
  side: Side,
  orderType: String,
  orderQuantity: BigDecimal,
  orderPrice: BigDecimal,
  currentExecutionType: BinanceExecutionType,
  orderId: Long,
  lastExecutedQuantity: BigDecimal,
  lastExecutedPrice: BigDecimal,
  commissionAmount: BigDecimal,
  commissionAsset: Option[String],
  transactionTime: Long,
  tradeId: Option[Long],
) extends BinanceStreamPayload

case class IrrelevantPayload(eventType: String) extends BinanceStreamPayload