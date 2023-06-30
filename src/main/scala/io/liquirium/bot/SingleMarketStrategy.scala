package io.liquirium.bot

import io.liquirium.core.CandleHistorySegment
import io.liquirium.core.OperationIntent.OrderIntent

import java.time.{Duration, Instant}

object SingleMarketStrategy {

  case class State(
    time: Instant,
    baseBalance: BigDecimal,
    quoteBalance: BigDecimal,
    candleHistory: CandleHistorySegment,
  )

}

trait SingleMarketStrategy extends (SingleMarketStrategy.State => Seq[OrderIntent]) {

  def candleLength: Duration

  // The SingleMarketBot will always provide a candle history of at least this length
  def minimumCandleHistoryLength: Duration

}
