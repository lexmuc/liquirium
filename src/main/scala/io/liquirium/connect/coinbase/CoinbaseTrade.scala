package io.liquirium.connect.coinbase

import io.liquirium.core.Side

import java.time.Instant

case class CoinbaseTrade(
  entryId: String,
  tradeId: String,
  orderId: String,
  tradeTime: Instant,
  tradeType: String,
  price: BigDecimal,
  size: BigDecimal,
  commission: BigDecimal,
  productId: String,
  sequenceTimestamp: Instant,
  side: Side,
)
