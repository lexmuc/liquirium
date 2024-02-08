package io.liquirium.bot

import io.liquirium.core.ExactResources
import io.liquirium.core.helpers.{BasicTest, MarketHelpers}
import io.liquirium.core.helpers.CoreHelpers.{dec, exchangeId}

import java.time.Instant

class SingleMarketStrategyBotRunConfigurationTest extends BasicTest {

  test("it makes initial balances available as a map") {
    val market = MarketHelpers.market(exchangeId(1), base = "base", quote = "quote")
    val runConfiguration = SingleMarketStrategyBotRunConfiguration(
      market = market,
      startTime = Instant.ofEpochSecond(0),
      endTimeOption = None,
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
