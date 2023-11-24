package io.liquirium.connect.deribit

case class DeribitInstrumentInfo(
  instrumentName: String,
  tickSize: BigDecimal,
  minTradeAmount: BigDecimal,
  contractSize: BigDecimal,
)
