package io.liquirium.bot.simulation

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.{dec, exchangeId, sec}
import io.liquirium.core.helpers.MarketHelpers.market
import io.liquirium.eval.Eval

class VisualizationMetricTest extends BasicTest {

  test("a market independent metric can be created from an eval") {
    val eval = Eval.unit(dec(123))
    val m = VisualizationMetric.marketIndependentMetric(eval)
    m.getEval(market = market(123), startTime = sec(123)) shouldBe theSameInstanceAs(eval)
  }

  test("a base balance metric can be created from a market and an initial balance") {
    val metricMarket = market(exchangeId(1), "base", "quote")
    val otherMarket = market(exchangeId(1), "quote", "other")
    val initialBalances = Map(metricMarket.baseLedger -> dec(1001))
    VisualizationMetric.baseBalanceMetric(
      tradeMarkets = Seq(metricMarket, otherMarket),
      initialBalances = initialBalances,
    ).getEval(
      market = metricMarket,
      startTime = sec(123),
    ) shouldEqual BalanceEval(
      ledgerRef = metricMarket.baseLedger,
      tradeMarkets = Seq(metricMarket, otherMarket),
      startTime = sec(123),
      initialBalance = dec(1001),
    )
  }

  test("a quote balance metric can be created from a market and an initial balance") {
    val metricMarket = market(exchangeId(1), "base", "quote")
    val otherMarket = market(exchangeId(1), "base", "other")
    val initialBalances = Map(metricMarket.quoteLedger -> dec(1002))
    VisualizationMetric.quoteBalanceMetric(
      tradeMarkets = Seq(metricMarket, otherMarket),
      initialBalances = initialBalances,
    ).getEval(
      market = metricMarket,
      startTime = sec(123),
    ) shouldEqual BalanceEval(
      ledgerRef = metricMarket.quoteLedger,
      tradeMarkets = Seq(metricMarket, otherMarket),
      startTime = sec(123),
      initialBalance = dec(1002),
    )
  }

}
