package io.liquirium.connect.deribit

import java.time.Instant

case class DeribitCandle(
  tick: Instant,
  open: BigDecimal,
  close: BigDecimal,
  high: BigDecimal,
  low: BigDecimal,
  volume: BigDecimal,
  cost: BigDecimal
)
