package io.liquirium.bot

import io.liquirium.core.{LedgerRef, Market}
import io.liquirium.util.TimePeriod


case class MultiMarketStrategyBotRunConfiguration(
  operationPeriod: TimePeriod,
  initialPricesByMarket: Map[Market, BigDecimal],
  initialBalances: Map[LedgerRef, BigDecimal],
) {

  def markets: Set[Market] = initialPricesByMarket.keySet

}
