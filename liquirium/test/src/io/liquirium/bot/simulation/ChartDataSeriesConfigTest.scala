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

  test("there is a default histogram series for onw trade volume (in the respective market)") {
    val seriesConfig = ChartDataSeriesConfig.ownTradeVolumeInMarketSeriesConfig()
    seriesConfig shouldEqual ChartDataSeriesConfig(
      precision = 8,
      caption = "Own trade volume",
      appearance = HistogramAppearance(
        color = "rgba(76, 175, 80, 0.5)",
      ),
      snapshotTime = ChartDataSeriesConfig.SnapshotTime.CandleEnd,
      metric = ChartMetric.latestCandleTradeVolumeMetric,
    )
  }

  test("some properties of the default histogram series for own trade volume can be overridden") {
    val seriesConfig = ChartDataSeriesConfig.ownTradeVolumeInMarketSeriesConfig(
      caption = "other caption",
      color = "blue",
      precision = 2,
    )
    seriesConfig.caption shouldEqual "other caption"
    seriesConfig.appearance.asInstanceOf[HistogramAppearance].color shouldEqual "blue"
    seriesConfig.precision shouldEqual 2
  }

}
