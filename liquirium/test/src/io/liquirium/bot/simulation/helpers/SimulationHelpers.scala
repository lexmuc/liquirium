package io.liquirium.bot.simulation.helpers

import io.liquirium.bot.simulation.{ChartDataSeriesConfig, ChartMetric, HistogramAppearance}
import io.liquirium.eval.helpers.EvalHelpers

object SimulationHelpers {

  def makeDataSeriesConfig(n: Int): ChartDataSeriesConfig =
    ChartDataSeriesConfig(
      precision = n,
      caption = s"caption$n",
      appearance = makeDataSeriesAppearance(n),
      snapshotTime =
        if (n % 2 == 1) ChartDataSeriesConfig.SnapshotTime.CandleStart
        else ChartDataSeriesConfig.SnapshotTime.CandleEnd,
      metric = ChartMetric.marketIndependentMetric(EvalHelpers.constant(BigDecimal(n))),
    )

  def makeDataSeriesAppearance(n: Int): HistogramAppearance =
    HistogramAppearance(
      color = s"color$n",
    )

}
