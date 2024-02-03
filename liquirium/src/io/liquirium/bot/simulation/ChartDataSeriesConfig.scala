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

  def ownTradeVolumeInMarketSeriesConfig(
    caption: String = "Own trade volume",
    color: String = "rgba(76, 175, 80, 0.5)",
    precision: Int = 8,
  ): ChartDataSeriesConfig = ChartDataSeriesConfig(
    precision = precision,
    caption = caption,
    appearance = HistogramAppearance(
      color = color,
    ),
    snapshotTime = SnapshotTime.CandleEnd,
    metric = ChartMetric.latestCandleTradeVolumeMetric,
  )

}

case class ChartDataSeriesConfig(
  precision: Int,
  caption: String,
  appearance: ChartDataSeriesAppearance,
  snapshotTime: SnapshotTime,
  metric: ChartMetric,
)
