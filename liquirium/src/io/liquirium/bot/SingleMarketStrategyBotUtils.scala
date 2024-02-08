package io.liquirium.bot

import io.liquirium.bot.simulation.ChartMetric
import io.liquirium.eval.Eval

object SingleMarketStrategyBotUtils {

  def candleBasedLastPriceEval(bot: SingleMarketStrategyBot): Eval[BigDecimal] = {
    bot.candleHistoryEval.map(_.lastPrice.getOrElse(bot.runConfiguration.initialPrice))
  }

  def totalValueEval(bot: SingleMarketStrategyBot, priceEval: Eval[BigDecimal]): Eval[BigDecimal] =
    for {
      base <- bot.baseBalanceEval
      quote <- bot.quoteBalanceEval
      p <- priceEval
    } yield quote + base * p

  def baseBalanceMetric(bot: SingleMarketStrategyBot): ChartMetric =
    ChartMetric.BaseBalanceMetric(
      tradeMarkets = Seq(bot.runConfiguration.market),
      initialBalances = bot.runConfiguration.initialBalances,
    )

  def quoteBalanceMetric(bot: SingleMarketStrategyBot): ChartMetric =
    ChartMetric.QuoteBalanceMetric(
      tradeMarkets = Seq(bot.runConfiguration.market),
      initialBalances = bot.runConfiguration.initialBalances,
    )

}
