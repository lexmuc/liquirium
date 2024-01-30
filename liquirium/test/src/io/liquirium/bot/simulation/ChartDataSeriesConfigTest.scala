package io.liquirium.bot.simulation

import io.liquirium.core.helpers.TestWithMocks

class ChartDataSeriesConfigTest extends TestWithMocks {

  test("there is a convenience factory for simple line series configs with reasonable defaults") {
    val metric = mock[ChartMetric]
    val seriesConfig = ChartDataSeriesConfig.simpleLineSeriesConfig(caption = "Simple series", metric = metric)
    seriesConfig shouldEqual ChartDataSeriesConfig(
      precision = 8,
      caption = "Simple series",
      appearance = LineAppearance(
        lineWidth = 1,
        color = "black",
        overlay = true,
      ),
      snapshotTime = ChartDataSeriesConfig.SnapshotTime.CandleEnd,
      metric = metric,
    )
  }

  test("some properties of the simple line series configs can be overridden") {
    val metric = mock[ChartMetric]
    val seriesConfig = ChartDataSeriesConfig.simpleLineSeriesConfig(
      caption = "Simple series",
      metric = metric,
      color = "#ff0000",
    )
    seriesConfig.appearance shouldEqual LineAppearance(
      lineWidth = 1,
      color = "#ff0000",
      overlay = true,
    )
  }

}
