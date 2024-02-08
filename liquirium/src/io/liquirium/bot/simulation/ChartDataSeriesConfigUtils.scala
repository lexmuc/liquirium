package io.liquirium.bot.simulation

import io.liquirium.bot.{HighestBuyEval, LowestSellEval}
import io.liquirium.bot.simulation.ChartDataSeriesConfig.SnapshotTime
import io.liquirium.core.{CandleHistorySegment, Market}
import io.liquirium.eval.Eval

import java.time.Instant

object ChartDataSeriesConfigUtils {

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

  def highestBuyConfig(fallbackEval: Eval[BigDecimal]): ChartDataSeriesConfig = ChartDataSeriesConfig(
    precision = 8,
    caption = "Highest Buy",
    appearance = LineAppearance(
      lineWidth = 2,
      color = "#00aa00",
      overlay = false,
    ),
    snapshotTime = SnapshotTime.CandleStart,
    metric = new ChartMetric {
      override def getEval(
        market: Market,
        startTime: Instant,
        chartCandlesEval: Eval[CandleHistorySegment],
      ): Eval[BigDecimal] =
        HighestBuyEval(market, fallback = fallbackEval)
    },
  )

  def lowestSellConfig(fallbackEval: Eval[BigDecimal]): ChartDataSeriesConfig = ChartDataSeriesConfig(
    precision = 8,
    caption = "Lowest Sell",
    appearance = LineAppearance(
      lineWidth = 2,
      color = "#ff0000",
      overlay = false,
    ),
    snapshotTime = SnapshotTime.CandleStart,
    metric = new ChartMetric {
      override def getEval(
        market: Market,
        startTime: Instant,
        chartCandlesEval: Eval[CandleHistorySegment],
      ): Eval[BigDecimal] =
        LowestSellEval(market, fallback = fallbackEval)
    },
  )

}
