package io.liquirium.bot

import io.liquirium.bot.simulation.ChartMetric
import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{dec, sec, secs}
import io.liquirium.core.helpers.{CandleHelpers, MarketHelpers, TestWithMocks}
import io.liquirium.eval.{BaseContext, Eval, Value}
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class SingleMarketStrategyBotUtilsTest extends TestWithMocks {

  test("a candle base last price eval is derived from the candles eval and falls back to the initial price") {
    val bot = mock(classOf[SingleMarketStrategyBot])
    bot.candleHistoryEval returns Eval.unit(
      candleHistorySegment(
        c5(start = sec(0), 1).copy(close = dec("1.23")),
        c5(start = sec(5), 2).copy(close = dec("2.34")),
      )
    )
    val (result1, _) = BaseContext.empty.evaluate(SingleMarketStrategyBotUtils.candleBasedLastPriceEval(bot))
    result1 shouldEqual Value(dec("2.34"))

    bot.candleHistoryEval returns Eval.unit(
      candleHistorySegment(
        CandleHelpers.emptyCandle(start = sec(0), length = secs(5)),
        CandleHelpers.emptyCandle(start = sec(5), length = secs(5)),
      )
    )
    val runConfig = mock(classOf[SingleMarketStrategyBotRunConfiguration])
    runConfig.initialPrice returns dec("1.23")
    bot.runConfiguration returns runConfig
    val (result2, _) = BaseContext.empty.evaluate(SingleMarketStrategyBotUtils.candleBasedLastPriceEval(bot))
    result2 shouldEqual Value(dec("1.23"))
  }

  test("the total value eval is calculated by adding the base value at a given price to the quote balance") {
    val bot = mock(classOf[SingleMarketStrategyBot])
    bot.baseBalanceEval returns Eval.unit(dec("2.0"))
    bot.quoteBalanceEval returns Eval.unit(dec("1.5"))
    val lastPriceEval = Eval.unit(dec("10.0"))
    val (result, _) = BaseContext.empty.evaluate(SingleMarketStrategyBotUtils.totalValueEval(bot, lastPriceEval))
    result shouldEqual Value(dec("1.5") + dec("2.0") * dec("10.0"))
  }

  test("a quote balance metric can be derived from the bot") {
    val bot = mock(classOf[SingleMarketStrategyBot])
    val market = MarketHelpers.market(1)
    val runConfig = mock(classOf[SingleMarketStrategyBotRunConfiguration])
    runConfig.market returns market
    val balances = Map(market.quoteLedger -> dec(123))
    runConfig.initialBalances returns balances
    bot.runConfiguration returns runConfig
    val metric = SingleMarketStrategyBotUtils.quoteBalanceMetric(bot)
    metric shouldEqual ChartMetric.QuoteBalanceMetric(
      tradeMarkets = Seq(market),
      initialBalances = balances,
    )
  }

  test("a base balance metric can be derived from the bot") {
    val bot = mock(classOf[SingleMarketStrategyBot])
    val market = MarketHelpers.market(1)
    val runConfig = mock(classOf[SingleMarketStrategyBotRunConfiguration])
    runConfig.market returns market
    val balances = Map(market.quoteLedger -> dec(123))
    runConfig.initialBalances returns balances
    bot.runConfiguration returns runConfig
    val metric = SingleMarketStrategyBotUtils.baseBalanceMetric(bot)
    metric shouldEqual ChartMetric.BaseBalanceMetric(
      tradeMarkets = Seq(market),
      initialBalances = balances,
    )
  }

}
