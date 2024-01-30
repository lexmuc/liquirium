package io.liquirium.bot.simulation.helpers

import io.liquirium.bot.simulation.{ChartDataSeriesAppearance, ChartDataSeriesConfig, ChartMetric, HistogramAppearance}
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.eval.helpers.EvalHelpers

object SimulationHelpers {

  def makeChartDataSeriesConfig(n: Int): ChartDataSeriesConfig =
    ChartDataSeriesConfig(
      precision = n,
      caption = s"caption$n",
      appearance = makeDataSeriesAppearance(n),
      snapshotTime =
        if (n % 2 == 1) ChartDataSeriesConfig.SnapshotTime.CandleStart
        else ChartDataSeriesConfig.SnapshotTime.CandleEnd,
      metric = ChartMetric.marketIndependentMetric(EvalHelpers.constant(BigDecimal(n))),
    )

  def makeChartDataSeriesConfig(
    caption: String = "",
    precision: Int = 1,
    appearance: ChartDataSeriesAppearance = makeDataSeriesAppearance(0),
  ): ChartDataSeriesConfig =
    ChartDataSeriesConfig(
      precision = precision,
      caption = caption,
      appearance = appearance,
      snapshotTime = ChartDataSeriesConfig.SnapshotTime.CandleStart,
      metric = ChartMetric.marketIndependentMetric(EvalHelpers.constant(dec(0))),
    )

  def makeDataSeriesAppearance(n: Int): HistogramAppearance =
    HistogramAppearance(
      color = s"color$n",
    )

}
