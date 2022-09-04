package io.liquirium.connect.binance

import io.liquirium.core.Side

case class BinanceOrder
(
  id: String,
  symbol: String,
  clientOrderId: String,
  price: BigDecimal,
  originalQuantity: BigDecimal,
  executedQuantity: BigDecimal,
  `type`: String,
  side: Side
) {

}
