package io.liquirium.core.orderTracking

import io.liquirium.core.Trade
import io.liquirium.core.orderTracking.OrderTrackingEvent.NewTrade
import io.liquirium.eval.IncrementalFoldHelpers.{IncrementalMetric, IncrementalSeqMetric}
import io.liquirium.eval.{Eval, IncrementalMap, IncrementalSeq}

object BasicOrderTrackingStateByIdEval {

  def apply(
    trades: Eval[IncrementalSeq[Trade]],
    openOrdersHistory: Eval[OpenOrdersHistory],
    successfulOperations: Eval[IncrementalSeq[OrderTrackingEvent.OperationEvent]],
  ): Eval[IncrementalMap[String, BasicOrderTrackingState]] = {
    val tradeEventsById = trades
      .filterIncremental(_.orderId.isDefined)
      .mapIncremental(NewTrade.apply)
      .groupByIncremental(_.t.orderId.get)
    val operationsById = successfulOperations.groupByIncremental(_.orderId)
    val allIdsMetric = allIds(tradeEventsById, openOrdersHistory.map(_.definedHistoriesById), operationsById)
    allStates(
      operationsWithOrdersMetric(
        orderHistoriesById = allOrderHistories(allIdsMetric, openOrdersHistory),
        operationsById = operationsById
      ),
      tradeEventsById,
    )
  }

  private def idsFromMaps[A, B](
    m1: Eval[IncrementalMap[String, A]],
    m2: Eval[IncrementalMap[String, B]],
  ): Eval[IncrementalMap[String, Unit]] =
    m1.mergeFoldIncremental(m2)((_, _) => IncrementalMap.empty[String, Unit]) {
      case (im, (oid, _)) => if (im.mapValue.contains(oid)) im else im.update(oid, ())
    } {
      case (im, (oid, _)) => if (im.mapValue.contains(oid)) im else im.update(oid, ())
    }

  private def allIds(
    tradeEvents: Eval[IncrementalMap[String, IncrementalSeq[NewTrade]]],
    orderHistoriesById: Eval[IncrementalMap[String, SingleOrderObservationHistory]],
    operationsById: Eval[IncrementalMap[String, IncrementalSeq[OrderTrackingEvent.OperationEvent]]],
  ): Eval[IncrementalMap[String, Unit]] =
    idsFromMaps(
      idsFromMaps(tradeEvents, operationsById),
      orderHistoriesById,
    )

  private def allOrderHistories(
    allIds: Eval[IncrementalMap[String, Unit]],
    openOrdersHistory: Eval[OpenOrdersHistory],
  ): Eval[IncrementalMap[String, SingleOrderObservationHistory]] =
    openOrdersHistory.map(_.emptySingleOrderHistory).flatMap { emptyHistory =>
      openOrdersHistory.map(_.definedHistoriesById).mergeFoldIncremental(allIds) {
        (_, _) => IncrementalMap.empty[String, SingleOrderObservationHistory]
      } {
        case (im, (id, hist)) => im.update(id, hist.get)
      } {
        case (im, (id, _)) => im.mapValue.get(id) match {
          case None => im.update(id, emptyHistory)
          case Some(_) => im
        }
      }
    }

  private def operationsWithOrdersMetric(
    orderHistoriesById: Eval[IncrementalMap[String, SingleOrderObservationHistory]],
    operationsById: Eval[IncrementalMap[String, IncrementalSeq[OrderTrackingEvent.OperationEvent]]],
  ): Eval[IncrementalMap[String, BasicOrderTrackingState]] =
    orderHistoriesById.mergeFoldIncremental(operationsById)(
      (_, _) => IncrementalMap.empty[String, BasicOrderTrackingState]
    ) {
      case (im, (id, newOrderHistory)) =>
        val newState = im.mapValue.get(id) match {
          case None => BasicOrderTrackingState(Seq(), newOrderHistory.get, Seq())
          case Some(s) => s.copy(observationHistory = newOrderHistory.get)
        }
        im.update(id, newState)
    } {
      case (im, (id, newOperations)) =>
        val newState = im.mapValue.get(id) match {
          case None => throw new RuntimeException("not supposed to happen")
          case Some(s) => s.copy(operationEvents = newOperations.get)
        }
        im.update(id, newState)
    }

  private def allStates(
    statesWithoutTrades: Eval[IncrementalMap[String, BasicOrderTrackingState]],
    tradesById: Eval[IncrementalMap[String, IncrementalSeq[NewTrade]]],
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
        val newState = im.mapValue.get(id) match {
          case None => throw new RuntimeException("not supposed to happen")
          case Some(s) => s.copy(tradeEvents = tradeEvents.get)
        }
        im.update(id, newState)
    }

}
