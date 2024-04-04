package io.liquirium.bot

import io.liquirium.core.{CandleHistorySegment, LedgerRef, Market}
import io.liquirium.core.OperationIntent.OrderIntent

import java.time.{Duration, Instant}

object MultiMarketStrategy {

  case class State(
    time: Instant,
    candleHistoriesByMarket: Map[Market, CandleHistorySegment],
    runConfiguration: MultiMarketStrategyBotRunConfiguration,
    balances: Map[LedgerRef, BigDecimal],
  )

}

trait MultiMarketStrategy extends (MultiMarketStrategy.State => Seq[OrderIntent]) {

  def candleLength: Duration

  // The strategy needs to see the past candles. This is how far.
  def minimumCandleHistoryLength: Duration

  def calculateInitialBalances(
    totalQuoteValue: BigDecimal,
    initialPrices: Map[Market, BigDecimal],
  ): Map[LedgerRef, BigDecimal]

}
