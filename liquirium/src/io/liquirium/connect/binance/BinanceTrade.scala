package io.liquirium.connect.binance

import java.time.Instant

case class BinanceTrade
(
  id: String,
  symbol: String,
  orderId: String,
  price: BigDecimal,
  quantity: BigDecimal,
  commission: BigDecimal,
  commissionAsset: String,
  time: Instant,
  isBuyer: Boolean,
)
