package io.liquirium.connect

import io.liquirium.core.CandleHistorySegment

import java.time.Instant

/**
 * Used by exchange connectors to determine the start time when requesting candles to extend a given candle history
 * segment. Candles may be available via the API only with a short delay or may changed retroactively so some overlap
 * with the know candles is recommended in order to ensure that all candles are received in their final state.
 */
trait CandleUpdateOverlapStrategy extends (CandleHistorySegment => Instant)

object CandleUpdateOverlapStrategy {

  def numberOfCandles(n: Int): CandleUpdateOverlapStrategy = new CandleUpdateOverlapStrategy {

    override def apply(segment: CandleHistorySegment): Instant = {
      val calculatedStart = segment.end minus (segment.candleLength multipliedBy n)
      if (calculatedStart isBefore segment.start) segment.start else calculatedStart
    }

  }

  def complete: CandleUpdateOverlapStrategy = new CandleUpdateOverlapStrategy {
    override def apply(segment: CandleHistorySegment): Instant = segment.start
  }

}



