package io.liquirium.connect.binance

case class BinanceSymbolInfo(
  symbol: String,
  tickSize: BigDecimal,
  minPrice: BigDecimal,
  maxPrice: BigDecimal,
  stepSize: BigDecimal,
  minQuantity: BigDecimal,
  maxQuantity: BigDecimal,
)
