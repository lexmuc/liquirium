package io.liquirium.bot

import io.liquirium.core.{CandleHistorySegment, ExactResources}
import io.liquirium.core.OperationIntent.OrderIntent

import java.time.{Duration, Instant}

object SingleMarketStrategy {

  case class State(
    time: Instant,
    baseBalance: BigDecimal,
    quoteBalance: BigDecimal,
    candleHistory: CandleHistorySegment,
    runConfiguration: SingleMarketStrategyBotRunConfiguration,
  )

}

trait SingleMarketStrategy extends (SingleMarketStrategy.State => Seq[OrderIntent]) {

  def candleLength: Duration

  // The strategy needs to see the past candles. This is how far.
  def minimumCandleHistoryLength: Duration

  def initialResources(totalQuoteValue: BigDecimal, initialPrice: BigDecimal): ExactResources

}
