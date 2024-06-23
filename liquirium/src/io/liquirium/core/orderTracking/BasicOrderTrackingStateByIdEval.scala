package io.liquirium.core.orderTracking

import io.liquirium.core.TradeHistorySegment
import io.liquirium.core.orderTracking.OrderTrackingEvent.NewTrade
import io.liquirium.eval.IncrementalFoldHelpers.{IncrementalEval, IncrementalSeqEval}
import io.liquirium.eval.{Eval, IncrementalMap, IncrementalSeq}

object BasicOrderTrackingStateByIdEval {

  def apply(
    trades: Eval[TradeHistorySegment],
    observationEvents: Eval[IncrementalSeq[OrderTrackingEvent.OrderObservationEvent]],
    successfulOperations: Eval[IncrementalSeq[OrderTrackingEvent.OperationEvent]],
  ): Eval[IncrementalMap[String, BasicOrderTrackingState]] = {
    val tradeEvents: Eval[IncrementalSeq[OrderTrackingEvent]] =
      trades
        .filterIncremental(_.orderId.nonEmpty)
        .mapIncremental(t => NewTrade(t))
    val tradesWithObservations = mergeSequences[OrderTrackingEvent](
      tradeEvents,
      observationEvents.asInstanceOf[Eval[IncrementalSeq[OrderTrackingEvent]]],
    )
    val allEvents = mergeSequences(
      tradesWithObservations,
      successfulOperations.asInstanceOf[Eval[IncrementalSeq[OrderTrackingEvent]]]
    )
    val groupedEvents = allEvents.groupByIncremental(_.orderId)
    val emptyMap = IncrementalMap.empty[String, BasicOrderTrackingState]
    groupedEvents.foldIncremental(_ => emptyMap) {
      case (im, (id, events)) =>
        val newState = BasicOrderTrackingState(
          operationEvents = events.get.collect { case o: OrderTrackingEvent.OperationEvent => o },
          tradeEvents = events.get.collect { case o: OrderTrackingEvent.NewTrade => o },
          observationEvents = events.get.collect { case o: OrderTrackingEvent.OrderObservationEvent => o },
        )
        im.update(id, newState)
    }
  }

  private def mergeSequences[E](
    a: Eval[IncrementalSeq[E]],
    b: Eval[IncrementalSeq[E]],
  ): Eval[IncrementalSeq[E]] = {
    a.mergeFoldIncremental(b)( (_, _) => IncrementalSeq.empty[E]) { case (x, e) => x.inc(e) }{ case (x, e) => x.inc(e) }
  }

}
