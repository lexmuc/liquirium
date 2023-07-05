package io.liquirium.bot

import io.liquirium.bot.BotInput._
import io.liquirium.core._
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval._

package object simulation {

  def singleMarketVisualizationLoggingStrategy(
    additionalCandleStartMetrics: Map[String, Eval[BigDecimal]] = Map.empty,
    additionalCandleEndMetrics: Map[String, Eval[BigDecimal]] = Map.empty,
  ) : SingleMarketLoggingStrategy[VisualizationLogger] =
    new SingleMarketLoggingStrategy[VisualizationLogger] {

      override def apply(bot: SingleMarketStrategyBot): VisualizationLogger = {
        val market = bot.market
        val strategy = bot.strategy
        // load more candles since indicators need some candle history
        val candlesForIndicatorsEval =
          InputEval(CandleHistoryInput(market, strategy.candleLength, bot.startTime.minusSeconds(3600 * 10)))

        val lastPriceEval = candlesForIndicatorsEval.map(_.lastPrice.get)

        val visualizationCandlesEval =
          CandleAggregationFold.aggregate(
            InputEval(CandleHistoryInput(market, strategy.candleLength, bot.startTime)),
            strategy.candleLength multipliedBy (12 * 4),
          )

        val tradesEval = InputEval(TradeHistoryInput(bot.market, bot.startTime))
        val baseBalanceEval = bot.tradeHistoryEval.foldIncremental(_ => bot.initialResources.baseBalance) {
          (bb, t) => bb + t.effects.filter(_.ledger == bot.market.baseLedger).map(_.change).sum
        }
        val quoteBalanceEval = bot.tradeHistoryEval.foldIncremental(_ => bot.initialResources.quoteBalance) {
          (qb, t) => qb + t.effects.filter(_.ledger == bot.market.quoteLedger).map(_.change).sum
        }
        val totalValueEval = for {
          base <- baseBalanceEval
          quote <- quoteBalanceEval
          p <- lastPriceEval
        } yield quote + base * p

        VisualizationLogger(
          candlesEval = visualizationCandlesEval,
          candleStartEvals = Map(
            "lastPrice" -> lastPriceEval,
            "lowestSell" -> LowestSellEval(bot.market, fallback = lastPriceEval),
            "highestBuy" -> HighestBuyEval(bot.market, fallback = lastPriceEval),
          ) ++ additionalCandleStartMetrics,
          candleEndEvals = Map(
            "ownTradeVolume" -> LatestCandleTradeVolumeEval(visualizationCandlesEval, tradesEval),
            "baseBalance" -> baseBalanceEval,
            "quoteBalance" -> quoteBalanceEval,
            "totalValue" -> totalValueEval,
          ) ++ additionalCandleEndMetrics,
        )

      }
    }

}
