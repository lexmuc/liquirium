package io.liquirium.connect.bitfinex

case class BitfinexPairInfo(
  pair: String,
  minOrderSize: BigDecimal,
  maxOrderSize: BigDecimal,
)
