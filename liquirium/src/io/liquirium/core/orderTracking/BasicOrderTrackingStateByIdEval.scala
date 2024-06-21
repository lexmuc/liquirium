package io.liquirium.core.orderTracking

import io.liquirium.core.TradeHistorySegment
import io.liquirium.core.orderTracking.OrderTrackingEvent.NewTrade
import io.liquirium.eval.IncrementalFoldHelpers.{IncrementalEval, IncrementalSeqEval}
import io.liquirium.eval.{Eval, IncrementalMap, IncrementalSeq}

object BasicOrderTrackingStateByIdEval {

  def apply(
    trades: Eval[TradeHistorySegment],
    openOrdersHistory: Eval[OpenOrdersHistory],
    successfulOperations: Eval[IncrementalSeq[OrderTrackingEvent.OperationEvent]],
  ): Eval[IncrementalMap[String, BasicOrderTrackingState]] = {
    val operationsById = successfulOperations.groupByIncremental(_.orderId)
    val emptyState = BasicOrderTrackingState(Seq(), Seq(), Seq())
    allStates(
      operationsWithOrdersEval(
        orderHistoriesById = openOrdersHistory.map(_.definedHistoriesById),
        operationsById = operationsById,
        emptyOrderTrackingState = emptyState,
      ),
      tradeEventsByIdEval(trades),
      emptyOrderTrackingState = emptyState,
    )
  }

  private def tradeEventsByIdEval(
    trades: Eval[TradeHistorySegment],
  ): Eval[IncrementalMap[String, IncrementalSeq[NewTrade]]] =
    trades
      .filterIncremental(_.orderId.isDefined)
      .mapIncremental(NewTrade.apply)
      .groupByIncremental(_.t.orderId.get)

  private def operationsWithOrdersEval(
    orderHistoriesById: Eval[IncrementalMap[String, SingleOrderObservationHistory]],
    operationsById: Eval[IncrementalMap[String, IncrementalSeq[OrderTrackingEvent.OperationEvent]]],
    emptyOrderTrackingState: BasicOrderTrackingState,
  ): Eval[IncrementalMap[String, BasicOrderTrackingState]] =
    orderHistoriesById.mergeFoldIncremental(operationsById)(
      (_, _) => IncrementalMap.empty[String, BasicOrderTrackingState]
    ) {
      case (im, (id, newOrderHistory)) =>
        val newState = im.mapValue.get(id) match {
          case None => BasicOrderTrackingState(Seq(), Seq(), newOrderHistory.get.changes)
          case Some(s) => s.copy(observationEvents = newOrderHistory.get.changes)
        }
        im.update(id, newState)
    } {
      case (im, (id, newOperations)) =>
        val newState = im.mapValue.getOrElse(id, emptyOrderTrackingState).copy(operationEvents = newOperations.get)
        im.update(id, newState)
    }

  private def allStates(
    statesWithoutTrades: Eval[IncrementalMap[String, BasicOrderTrackingState]],
    tradesById: Eval[IncrementalMap[String, IncrementalSeq[NewTrade]]],
    emptyOrderTrackingState: BasicOrderTrackingState,
  ): Eval[IncrementalMap[String, BasicOrderTrackingState]] =
    statesWithoutTrades.mergeFoldIncremental(tradesById)(
      (_, _) => IncrementalMap.empty[String, BasicOrderTrackingState]
    ) {
      case (im, (id, stateUpdate)) =>
        val newState = im.mapValue.get(id) match {
          case None => stateUpdate.get
          case Some(s) => stateUpdate.get.copy(tradeEvents = s.tradeEvents)
        }
        im.update(id, newState)
    } {
      case (im, (id, tradeEvents)) =>
        val newState = im.mapValue.getOrElse(id, emptyOrderTrackingState).copy(tradeEvents = tradeEvents.get)
        im.update(id, newState)
    }

}
