package io.liquirium.bot.simulation

import io.liquirium.core.Candle

case class VisualizationUpdate(
  candle: Candle,
  namedDataPoints: Map[String, BigDecimal]
)
