package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.ChartDataSeriesConfig.SnapshotTime


object ChartDataSeriesConfig {

  sealed trait SnapshotTime

  object SnapshotTime {
    case object CandleStart extends SnapshotTime
    case object CandleEnd extends SnapshotTime
  }

  def simpleLineSeriesConfig(
    caption: String,
    metric: ChartMetric,
    color: String = "black",
  ): ChartDataSeriesConfig = ChartDataSeriesConfig(
    precision = 8,
    caption = caption,
    appearance = LineAppearance(
      lineWidth = 1,
      color = color,
      overlay = true,
    ),
    snapshotTime = SnapshotTime.CandleEnd,
    metric = metric,
  )

}

case class ChartDataSeriesConfig(
  precision: Int,
  caption: String,
  appearance: ChartDataSeriesAppearance,
  snapshotTime: SnapshotTime,
  metric: ChartMetric,
)
