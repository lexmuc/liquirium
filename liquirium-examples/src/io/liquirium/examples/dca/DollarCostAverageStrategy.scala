package io.liquirium.examples.dca

import io.liquirium.bot.SingleMarketStrategyBotUtils.{baseBalanceMetric, quoteBalanceMetric, totalValueEval}
import io.liquirium.bot.simulation.ChartDataSeriesConfigUtils.{ownTradeVolumeInMarketSeriesConfig, simpleLineSeriesConfig}
import io.liquirium.bot.simulation.ChartMetric.marketIndependentMetric
import io.liquirium.bot.simulation.{BotSimulationInfo, ChartDataSeriesConfigUtils}
import io.liquirium.bot.{SingleMarketStrategy, SingleMarketStrategyBot, SingleMarketStrategyBotUtils}
import io.liquirium.core.{ExactResources, OperationIntent}
import io.liquirium.eval.Eval

import java.time.Duration


object DollarCostAverageStrategy {

  // We define some data series that we want to see in the chart.
  def getSimulationInfo(bot: SingleMarketStrategyBot): BotSimulationInfo = {
    val lastPriceEval: Eval[BigDecimal] = SingleMarketStrategyBotUtils.candleBasedLastPriceEval(bot)
    val chartDataSeries = Seq(
      simpleLineSeriesConfig("Total value", marketIndependentMetric(totalValueEval(bot, lastPriceEval))),
      ChartDataSeriesConfigUtils.highestBuyConfig(lastPriceEval.map(_ * BigDecimal(0.9))),
      simpleLineSeriesConfig("Base balance", baseBalanceMetric(bot), color = "blue"),
      simpleLineSeriesConfig("Quote balance", quoteBalanceMetric(bot), color = "orange"),
      ownTradeVolumeInMarketSeriesConfig(),
    )
    BotSimulationInfo(
      basicCandleLength = bot.strategy.candleLength,
      chartDataSeriesConfigs = chartDataSeries,
      markets = Seq(bot.runConfiguration.market),
    )
  }

}


/**
 * @param duration     Many strategies can run forever but dollar cost averaging needs a fixed duration since it will
 *                     run out of money eventually.
 * @param candleLength One hour candles should be enough for dollar cost averaging
 */
case class DollarCostAverageStrategy(
  duration: Duration,
  candleLength: Duration,
) extends SingleMarketStrategy {

  override def minimumCandleHistoryLength: Duration = {
    // We don't need any more history because we are not relying on any indicators calculated for the past
    Duration.ofMinutes(600)
  }

  override def initialResources(totalQuoteValue: BigDecimal, initialPrice: BigDecimal): ExactResources = {
    // We have not bought the asset yet. We only have quote currency.
    ExactResources(
      baseBalance = BigDecimal(0),
      quoteBalance = totalQuoteValue,
    )
  }

  /**
   * This is the main method of the strategy. It is called at least every time a new candle is available.
   * The strategy can access the state of the bot comprising the candle history, current time and balances.
   *
   * Output is a sequence of order intents. The bot will try to convert these into actual (limit) orders.
   * If the an order is already in the order book, it will be ignored. If the order is not possible, for instance
   * because the volume is too small, it will be ignored. Open orders not in the sequence will be cancelled.
   * In short, the sequence should contain all orders that the bot should have in the order book.
   */
  override def apply(state: SingleMarketStrategy.State): Seq[OperationIntent.OrderIntent] = {
    // in case last price is None (no trade activity in the market yet), we return an empty sequence
    state.candleHistory.lastPrice map { price =>
      val buyPrice = price * BigDecimal(0.995) // slightly lower than the current price to avoid taker fee
      // We return one operation intent. Liquirium will try to convert this into an actual order and cancel other
      // orders. If the order is not possible, for instance because the volume is too small, it will be ignored.
      OperationIntent.OrderIntent(
        quantity = getSpendingObligation(state) / buyPrice,
        price = buyPrice,
      )
    }
  }.toSeq

  // How much money should we have spent at this point in time?
  private def getSpendingTarget(state: SingleMarketStrategy.State): BigDecimal = {
    val startMoney = state.runConfiguration.initialResources.quoteBalance
    val secondsPassed = Duration.between(state.runConfiguration.operationPeriod.start, state.time).toSeconds
    val secondsTotal = duration.toSeconds
    startMoney.toDouble / secondsTotal.toDouble * secondsPassed.toDouble
  }

  // How much money should we spend now to meet the target?
  private def getSpendingObligation(state: SingleMarketStrategy.State): BigDecimal = {
    val spentMoney = state.runConfiguration.initialResources.quoteBalance - state.quoteBalance
    // never return a negative number
    (getSpendingTarget(state) - spentMoney).max(BigDecimal(0))
  }

}
