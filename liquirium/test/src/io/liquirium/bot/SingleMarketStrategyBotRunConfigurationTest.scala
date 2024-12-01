package io.liquirium.bot

import io.liquirium.core.ExactResources
import io.liquirium.core.helpers.{BasicTest, MarketHelpers}
import io.liquirium.core.helpers.CoreHelpers.{dec, exchangeId, sec}
import io.liquirium.util.TimePeriod
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class SingleMarketStrategyBotRunConfigurationTest extends BasicTest {

  test("it makes initial balances available as a map") {
    val market = MarketHelpers.market(exchangeId(1), base = "base", quote = "quote")
    val runConfiguration = SingleMarketStrategyBotRunConfiguration(
      market = market,
      operationPeriod = TimePeriod(sec(0), sec(1000)),
      initialPrice = BigDecimal(1),
      initialResources = ExactResources.apply(
        baseBalance = dec(1),
        quoteBalance = dec(2),
      ),
    )
    runConfiguration.initialBalances shouldEqual Map(
      market.baseLedger -> dec(1),
      market.quoteLedger -> dec(2),
    )
  }

}
