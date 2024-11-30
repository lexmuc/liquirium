package io.liquirium.bot.simulation

import io.liquirium.bot.{HighestBuyEval, LowestSellEval}
import io.liquirium.bot.simulation.ChartDataSeriesConfig.SnapshotTime
import io.liquirium.core.CandleHistorySegment
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.{MarketHelpers, TestWithMocks}
import io.liquirium.eval.Eval
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class ChartDataSeriesConfigUtilsTest extends TestWithMocks {

  test("there is a convenience factory for simple line series configs with reasonable defaults") {
    val metric = mock(classOf[ChartMetric])
    val seriesConfig = ChartDataSeriesConfigUtils.simpleLineSeriesConfig(caption = "Simple series", metric = metric)
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
    val metric = mock(classOf[ChartMetric])
    val seriesConfig = ChartDataSeriesConfigUtils.simpleLineSeriesConfig(
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
    val seriesConfig = ChartDataSeriesConfigUtils.ownTradeVolumeInMarketSeriesConfig()
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
    val seriesConfig = ChartDataSeriesConfigUtils.ownTradeVolumeInMarketSeriesConfig(
      caption = "other caption",
      color = "blue",
      precision = 2,
    )
    seriesConfig.caption shouldEqual "other caption"
    seriesConfig.appearance.asInstanceOf[HistogramAppearance].color shouldEqual "blue"
    seriesConfig.precision shouldEqual 2
  }

  test("a highest buy config can be created for a given fallback eval") {
    val fallbackEval = mock(classOf[Eval[BigDecimal]])

    val highestBuyConfig = ChartDataSeriesConfigUtils.highestBuyConfig(fallbackEval)
    highestBuyConfig.precision shouldEqual 8
    highestBuyConfig.caption shouldEqual "Highest Buy"
    highestBuyConfig.appearance shouldEqual LineAppearance(
      lineWidth = 2,
      color = "#00aa00",
      overlay = false,
    )
    highestBuyConfig.snapshotTime shouldEqual SnapshotTime.CandleStart

    val market = MarketHelpers.market(1)
    highestBuyConfig.metric.getEval(market, sec(0), mock(classOf[Eval[CandleHistorySegment]])) shouldEqual
      HighestBuyEval(market, fallbackEval)
  }

  test("a lowest sell config can be created for a given fallback eval") {
    val fallbackEval = mock(classOf[Eval[BigDecimal]])

    val lowestSellConfig = ChartDataSeriesConfigUtils.lowestSellConfig(fallbackEval)
    lowestSellConfig.precision shouldEqual 8
    lowestSellConfig.caption shouldEqual "Lowest Sell"
    lowestSellConfig.appearance shouldEqual LineAppearance(
      lineWidth = 2,
      color = "#ff0000",
      overlay = false,
    )
    lowestSellConfig.snapshotTime shouldEqual SnapshotTime.CandleStart

    val market = MarketHelpers.market(1)
    lowestSellConfig.metric.getEval(market, sec(0), mock(classOf[Eval[CandleHistorySegment]])) shouldEqual
      LowestSellEval(market, fallbackEval)
  }

}
