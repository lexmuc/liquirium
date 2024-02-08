package io.liquirium.bot

import io.liquirium.core.{ExactResources, LedgerRef, Market}

import java.time.Instant

case class SingleMarketStrategyBotRunConfiguration(
  market: Market,
  startTime: Instant,
  endTimeOption: Option[Instant],
  initialPrice: BigDecimal,
  initialResources: ExactResources,
) {

  val initialBalances: Map[LedgerRef, BigDecimal] = Map(
    market.baseLedger -> initialResources.baseBalance,
    market.quoteLedger -> initialResources.quoteBalance,
  )

}
