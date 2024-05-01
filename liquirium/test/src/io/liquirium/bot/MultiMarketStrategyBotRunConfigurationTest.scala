package io.liquirium.bot

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.MarketHelpers.market
import io.liquirium.util.TimePeriod

class MultiMarketStrategyBotRunConfigurationTest extends BasicTest {

  test("it derives markets from the initial prices and makes them available as a set") {
    MultiMarketStrategyBotRunConfiguration(
      operationPeriod = TimePeriod(sec(0), sec(1000)),
      initialPricesByMarket = Map(
        market(1) -> BigDecimal(1),
        market(2) -> BigDecimal(2),
      ),
      initialBalances = Map(
        market(1).quoteLedger -> dec(1),
        market(1).baseLedger -> dec(1),
        market(2).quoteLedger -> dec(1),
        market(2).baseLedger -> dec(1),
      ),
      initialValue = dec(1),
    ).markets shouldEqual Set(market(1), market(2))
  }

}
