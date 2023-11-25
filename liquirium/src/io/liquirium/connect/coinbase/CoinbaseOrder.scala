package io.liquirium.connect.coinbase

import io.liquirium.core.Side

case class CoinbaseOrder(
  orderId: String,
  productId: String,
  fullQuantity: BigDecimal,
  filledQuantity: BigDecimal,
  side: Side,
  price: BigDecimal,
) {
  def openQuantity: BigDecimal = fullQuantity - filledQuantity
}

