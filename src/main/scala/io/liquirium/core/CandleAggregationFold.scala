package io.liquirium.core

import io.liquirium.core.CandleAggregationFold.AggregationState
import io.liquirium.eval.IncrementalFold

import java.time.{Duration, Instant}
import scala.annotation.tailrec

object CandleAggregationFold {

  @tailrec
  private def padBatch(cc: Seq[Candle], length: Int): Seq[Candle] = {
    if (cc.size < length) {
      val newStart = cc.head.startTime minus cc.head.length
      val emptyCandle: Candle = Candle.empty(newStart, cc.head.length)
      padBatch(emptyCandle +: cc, length)
    }
    else cc
  }

  def startSegment(baseValue: CandleHistorySegment, aggregateLength: Duration): CandleHistorySegment = {
    val baseMillis = baseValue.candleLength.toMillis
    val aggregateMillis = aggregateLength.toMillis
    if (aggregateMillis % baseMillis != 0) {
      val baseLength = baseValue.candleLength
      throw new RuntimeException(s"Cannot aggregate candle length $baseLength to $aggregateLength")
    }
    val baseStartMillis = baseValue.start.toEpochMilli
    val newStartMillis = baseStartMillis - baseStartMillis % aggregateMillis
    CandleHistorySegment.empty(Instant.ofEpochMilli(newStartMillis), aggregateLength)
  }

  case class AggregationState(completedAggregates: CandleHistorySegment, buffer: Seq[Candle]) {

    def addCandle(c: Candle): AggregationState = {
      val extendedBuffer = buffer :+ c
      val batchLength: Int = (completedAggregates.candleLength.toMillis / c.length.toMillis).toInt
      if (c.endTime.toEpochMilli % completedAggregates.candleLength.toMillis == 0) {

        val newAggregate = Candle.aggregate(padBatch(extendedBuffer, batchLength))
        AggregationState(
          completedAggregates = completedAggregates.append(newAggregate),
          buffer = Seq()
        )
      }
      else copy(buffer = extendedBuffer)
    }

  }

}

case class CandleAggregationFold(aggregateLength: Duration)
  extends IncrementalFold[Candle, CandleHistorySegment, AggregationState] {

  override def startValue(baseValue: CandleHistorySegment): AggregationState =
    AggregationState(CandleAggregationFold.startSegment(baseValue, aggregateLength), Seq())

  override def step(oldValue: AggregationState, increment: Candle): AggregationState = oldValue.addCandle(increment)

}
