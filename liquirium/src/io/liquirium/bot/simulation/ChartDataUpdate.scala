package io.liquirium.bot.simulation

import io.liquirium.core.Candle

case class ChartDataUpdate(
  candle: Candle,
  namedDataPoints: Map[Int, BigDecimal]
)
