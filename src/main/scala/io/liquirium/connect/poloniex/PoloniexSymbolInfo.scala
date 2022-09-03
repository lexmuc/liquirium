package io.liquirium.connect.poloniex

/**
 * @param priceScale refers to the max number of decimals allowed -> 2 implies accepted values like 200.34, 13.2, 100
 */

case class PoloniexSymbolInfo(
  symbol: String,
  priceScale: Int,
  quantityScale: Int,
  minAmount: BigDecimal,
  minQuantity: BigDecimal,
)
