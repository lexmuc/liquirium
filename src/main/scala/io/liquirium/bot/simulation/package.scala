package io.liquirium.bot

import io.liquirium.bot.BotInput._
import io.liquirium.core._
import io.liquirium.eval._

package object simulation {

  def singleMarketVisualizationLoggingStrategy(
    additionalCandleStartMetrics: Map[String, Eval[BigDecimal]] = Map.empty,
    additionalCandleEndMetrics: Map[String, Eval[BigDecimal]] = Map.empty,
  ) : SingleMarketLoggingStrategy[VisualizationLogger] =
    new SingleMarketLoggingStrategy[VisualizationLogger] {

      override def apply(bot: SingleMarketStrategyBot): VisualizationLogger = {
        val market = bot.runConfiguration.market
        val startTime = bot.runConfiguration.startTime
        val strategy = bot.strategy
        // load more candles since indicators need some candle history
        val candlesForIndicatorsEval =
          InputEval(CandleHistoryInput(market, strategy.candleLength, startTime.minusSeconds(3600 * 10)))

        val lastPriceEval = candlesForIndicatorsEval.map(_.lastPrice.get)

        val visualizationCandlesEval =
          CandleAggregationFold.aggregate(
            InputEval(CandleHistoryInput(market, strategy.candleLength, startTime)),
            strategy.candleLength multipliedBy (12 * 4),
          )

        VisualizationLogger(
          candlesEval = visualizationCandlesEval,
          candleStartEvals = Map(
            "lastPrice" -> lastPriceEval,
            "lowestSell" -> LowestSellEval(market, fallback = lastPriceEval),
            "highestBuy" -> HighestBuyEval(market, fallback = lastPriceEval),
          ) ++ additionalCandleStartMetrics,
          candleEndEvals = Map(
            "ownTradeVolume" -> LatestCandleTradeVolumeEval(visualizationCandlesEval, bot.tradeHistoryEval),
          ) ++ additionalCandleEndMetrics,
        )

      }
    }

}
