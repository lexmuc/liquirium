package io.liquirium.connect.coinbase

/**
 * @param baseIncrement Minimum amount base value can be increased or decreased at once.
 */

case class CoinbaseProductInfo(
  symbol: String,
  baseIncrement: BigDecimal,
  baseMinSize: BigDecimal,
  baseMaxSize: BigDecimal,
  quoteIncrement: BigDecimal,
  quoteMinSize: BigDecimal,
  quoteMaxSize: BigDecimal,
)
