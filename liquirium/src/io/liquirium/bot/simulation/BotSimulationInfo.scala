package io.liquirium.bot.simulation

import io.liquirium.core.Market

import java.time.Duration

case class BotSimulationInfo(
  basicCandleLength: Duration,
  chartDataSeriesConfigs: Seq[ChartDataSeriesConfig],
  markets: Seq[Market],
)
