package io.liquirium.bot

import io.liquirium.core.{ExactResources, LedgerRef, Market}
import io.liquirium.util.TimePeriod

case class SingleMarketStrategyBotRunConfiguration(
  market: Market,
  operationPeriod: TimePeriod,
  initialPrice: BigDecimal,
  initialResources: ExactResources,
) {

  val initialBalances: Map[LedgerRef, BigDecimal] = Map(
    market.baseLedger -> initialResources.baseBalance,
    market.quoteLedger -> initialResources.quoteBalance,
  )

}
