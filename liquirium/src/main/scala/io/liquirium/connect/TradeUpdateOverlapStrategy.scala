package io.liquirium.connect

import io.liquirium.core.TradeHistorySegment

import java.time.{Duration, Instant}

/**
 * Used by exchange connectors to determine the start time when requesting trades to extend a given trade history
 * segment. Trades may be available via the API only with a short delay so some overlap with the know trades
 * is recommended in order to ensure that no trades are missed.
 */
trait TradeUpdateOverlapStrategy extends (TradeHistorySegment => Instant)

object TradeUpdateOverlapStrategy {

  def complete: TradeUpdateOverlapStrategy = new TradeUpdateOverlapStrategy {
    override def apply(segment: TradeHistorySegment): Instant = segment.start
  }

  def fixedOverlap(duration: Duration): TradeUpdateOverlapStrategy = new TradeUpdateOverlapStrategy {
    override def apply(segment: TradeHistorySegment): Instant = {
      val calculatedStart = segment.end minus duration
      if (calculatedStart.isBefore(segment.start)) segment.start else calculatedStart
    }
  }

}
