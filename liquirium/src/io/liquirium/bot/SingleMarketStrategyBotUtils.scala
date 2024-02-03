package io.liquirium.bot

import io.liquirium.bot.BotInput.CandleHistoryInput
import io.liquirium.bot.simulation.ChartDataSeriesConfig.{SnapshotTime, ownTradeVolumeInMarketSeriesConfig, simpleLineSeriesConfig}
import io.liquirium.bot.simulation.{ChartDataSeriesConfig, ChartMetric, HistogramAppearance, LineAppearance}
import io.liquirium.bot.simulation.ChartMetric.marketIndependentMetric
import io.liquirium.core.{CandleHistorySegment, LedgerRef, Market}
import io.liquirium.eval.{Eval, InputEval}

import java.time.Instant

// #TODO refactor and test
object SingleMarketStrategyBotUtils {

  def getDataSeriesConfigs(coreBot: SingleMarketStrategyBot): Seq[ChartDataSeriesConfig] = {
    val lastPriceEval: Eval[BigDecimal] = {
      val market = coreBot.runConfiguration.market
      val startTime = coreBot.runConfiguration.startTime
      val candlesForIndicatorsEval = InputEval(
        CandleHistoryInput(
          market = market,
          candleLength = coreBot.strategy.candleLength,
          start = startTime minus coreBot.strategy.minimumCandleHistoryLength,
        ),
      )
      candlesForIndicatorsEval.map(_.lastPrice.get)
    }

    // benchmark depends on the concrete strategy, does not belong here
    val benchmarkEval: Eval[BigDecimal] = {
      val initialResources = coreBot.runConfiguration.initialResources
      val initialInferredPrice = initialResources.quoteBalance / initialResources.baseBalance
      val initialValue = initialResources.valueAt(initialInferredPrice)
      for {
        p <- lastPriceEval
      } yield {
        val benchmarkChange = Math.sqrt(p.toDouble / initialInferredPrice.toDouble)
        initialValue * benchmarkChange
      }
    }

    // #TODO move to core bot
    val initialBalances: Map[LedgerRef, BigDecimal] = Map(
      coreBot.runConfiguration.market.baseLedger -> coreBot.runConfiguration.initialResources.baseBalance,
      coreBot.runConfiguration.market.quoteLedger -> coreBot.runConfiguration.initialResources.quoteBalance,
    )

    val totalValueEval: Eval[BigDecimal] =
      for {
        base <- coreBot.baseBalanceEval
        quote <- coreBot.quoteBalanceEval
        p <- lastPriceEval
      } yield quote + base * p

    val markets = Seq(coreBot.runConfiguration.market)

    val lowestSellConfig = ChartDataSeriesConfig(
      precision = 8,
      caption = "Lowest Sell",
      appearance = LineAppearance(
        lineWidth = 2,
        color = "#ff0000",
        overlay = false,
      ),
      snapshotTime = SnapshotTime.CandleStart,
      metric = new ChartMetric {
        override def getEval(market: Market, startTime: Instant, chartCandlesEval: Eval[CandleHistorySegment]): Eval[BigDecimal] =
          LowestSellEval(market, fallback = lastPriceEval)
      },
    )

    val highestBuyConfig = ChartDataSeriesConfig(
      precision = 8,
      caption = "Highest Buy",
      appearance = LineAppearance(
        lineWidth = 2,
        color = "#00aa00",
        overlay = false,
      ),
      snapshotTime = SnapshotTime.CandleStart,
      metric = new ChartMetric {
        override def getEval(market: Market, startTime: Instant, chartCandlesEval: Eval[CandleHistorySegment]): Eval[BigDecimal] =
          HighestBuyEval(market, fallback = lastPriceEval)
      },
    )

    Seq(
      simpleLineSeriesConfig("Total value", marketIndependentMetric(totalValueEval)),
      simpleLineSeriesConfig("Benchmark", marketIndependentMetric(benchmarkEval), color = "orange"),
      simpleLineSeriesConfig(
        "Benchmark ratio",
        marketIndependentMetric(Eval.map2(totalValueEval, benchmarkEval)(_ / _)),
        color = "brown",
      ),
      simpleLineSeriesConfig(
        "Base balance", ChartMetric.baseBalanceMetric(
        tradeMarkets = markets,
        initialBalances = initialBalances,
      ), color = "blue"),
      simpleLineSeriesConfig("Quote balance", ChartMetric.quoteBalanceMetric(
        tradeMarkets = markets,
        initialBalances = initialBalances,
      )),
      lowestSellConfig,
      highestBuyConfig,
      ownTradeVolumeInMarketSeriesConfig(),
    )
  }

}
