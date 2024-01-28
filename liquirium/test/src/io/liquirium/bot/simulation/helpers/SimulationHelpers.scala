package io.liquirium.bot.simulation.helpers

import io.liquirium.bot.simulation.{DataSeriesConfig, HistogramAppearance}

object SimulationHelpers {

  def makeDataSeriesConfig(n: Int): DataSeriesConfig =
    DataSeriesConfig(
      key = s"key$n",
      precision = n,
      caption = s"caption$n",
      appearance = makeDataSeriesAppearance(n),
    )

  def makeDataSeriesAppearance(n: Int): HistogramAppearance =
    HistogramAppearance(
      color = s"color$n",
    )

}
