package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.ChartDataSeriesConfig.SnapshotTime


object ChartDataSeriesConfig {

  sealed trait SnapshotTime

  object SnapshotTime {
    case object CandleStart extends SnapshotTime
    case object CandleEnd extends SnapshotTime
  }


}

case class ChartDataSeriesConfig(
  precision: Int,
  caption: String,
  appearance: ChartDataSeriesAppearance,
  snapshotTime: SnapshotTime,
  metric: ChartMetric,
)
