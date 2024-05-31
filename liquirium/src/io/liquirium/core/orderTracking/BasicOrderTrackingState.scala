package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.orderTracking.BasicOrderTrackingState.SyncReason._
import io.liquirium.core.orderTracking.BasicOrderTrackingState._
import io.liquirium.core.orderTracking.rules._
import io.liquirium.util.AbsoluteQuantity

import java.time.Instant


object BasicOrderTrackingState {

  sealed trait SyncReason

  sealed trait ErrorState

  object SyncReason {

    case class UnknownWhyOrderIsGone(time: Instant) extends SyncReason

    case class ExpectingTrades(time: Instant, quantity: BigDecimal) extends SyncReason

    case class ExpectingObservationChange(since: Instant, observation: Option[Order]) extends SyncReason

    case class ExpectingOrderToAppear(since: Instant, order: Order) extends SyncReason

    case class UnknownIfMoreTradesBeforeCancel(time: Instant) extends SyncReason

  }

  object ErrorState {

    case class InconsistentEvents(eventA: OrderTrackingEvent, eventB: OrderTrackingEvent) extends ErrorState

    case class ReappearingOrderInconsistency(event: OrderTrackingEvent) extends ErrorState

    case class Overfill(event: OrderTrackingEvent.NewTrade, totalFill: BigDecimal, maxFill: BigDecimal)
      extends ErrorState

  }

}

case class BasicOrderTrackingState(
  operationEvents: Seq[OrderTrackingEvent.OperationEvent],
  tradeEvents: Seq[OrderTrackingEvent.NewTrade],
  observationChanges: Seq[OrderTrackingEvent.ObservationChange],
) {

  val creationEvents: Seq[OrderTrackingEvent.Creation] =
    operationEvents.collect { case c: OrderTrackingEvent.Creation => c }

  val creation: Option[OrderTrackingEvent.Creation] =
    creationEvents.foldLeft(Option.empty[OrderTrackingEvent.Creation]) {
      case (None, c) => Some(c)
      case (Some(c1), c2) if c1.timestamp.isAfter(c2.timestamp) => Some(c1)
      case (_, c2) => Some(c2)
    }

  val cancellationEvents: Seq[OrderTrackingEvent.Cancel] =
    operationEvents.collect { case c: OrderTrackingEvent.Cancel => c }
  val cancellation: Option[OrderTrackingEvent.Cancel] = cancellationEvents.headOption

  val totalTradeQuantity: BigDecimal = tradeEvents.map(_.t.quantity).sum

  val latestPresentObservationChange: Option[OrderTrackingEvent.ObservationChange] =
    observationChanges.filter(_.order.isDefined).lastOption

  val latestPresentState: Option[Order] = latestPresentObservationChange.flatMap(_.order)

  val orderWithFullQuantity: Option[Order] =
    latestPresentState.map(_.resetQuantity) orElse creation.map(_.order.resetQuantity)

  val isCurrentlyObserved: Boolean = observationChanges.lastOption.exists(_.order.isDefined)

  private val consistencyRules = Seq(
    AtMostOneCreation,
    SameFullQuantityInObservations,
    NoQuantityIncrease,
    FullQuantityAtCreationMatchesTheLatestPresentObservation,
    CancelsAreConsistentWithOtherEvents,
    OrderDoesNotReappear,
    OrderIsNotOverfilled,
  )

  val errorState: Option[ErrorState] =
    consistencyRules
      .iterator.map(_.check(this))
      .collectFirst({ case Some(e) => e })

  val syncReasons: Set[SyncReason] = latestPresentState match {
    case None => syncReasonsIfNeverObserved()
    case Some(o) => syncReasonsIfObserved(o.resetQuantity)
  }

  val reportingState: Option[Order] =
    if (!isCurrentlyObserved || cancellation.isDefined) None
    else latestPresentState match {
      case Some(lastObservedOrder) if totalTradeQuantity.abs < lastObservedOrder.fullQuantity.abs =>
        Some(lastObservedOrder.resetQuantity.reduceQuantity(totalTradeQuantity))
      case _ => None
    }

  private def syncReasonsIfNeverObserved(): Set[SyncReason] = {
    if (tradeEvents.nonEmpty && creation.isEmpty && cancellation.isEmpty) {
      Set(UnknownWhyOrderIsGone(tradeEvents.last.timestamp))
    }
    else if (creation.isDefined && cancellation.isEmpty) {
      val reducedOrder = orderWithFullQuantity.get.reduceQuantity(totalTradeQuantity)
      if (reducedOrder.openQuantity.abs > 0) {
        val timestamp = (tradeEvents.map(_.timestamp) ++ creation.map(_.timestamp)).max
        val impliedTradeQuantity = creation.get.order.filledQuantity
        if (impliedTradeQuantity.abs > totalTradeQuantity.abs)
          Set(
            ExpectingTrades(timestamp, impliedTradeQuantity - totalTradeQuantity),
            ExpectingOrderToAppear(timestamp, creation.get.order),
          )
        else
          Set(
            ExpectingOrderToAppear(timestamp, reducedOrder),
          )

      }
      else Set()
    }
    else {
      val optReason = cancellation flatMap {
        c =>
          if (orderWithFullQuantity.isDefined && c.absoluteRestQuantity.isDefined) {
            None
          }
          else {
            Some(UnknownIfMoreTradesBeforeCancel(c.timestamp))
          }
      }
      optReason.toSet
    }
  }

  private def syncReasonsIfObserved(fullOrder: Order): Set[SyncReason] = {
    val filledQuantity = latestPresentState.map(_.filledQuantity) getOrElse BigDecimal(0)

    val optionalExpectingTrades: Option[ExpectingTrades] = {
      val impliedTradeQuantityFromCancelWithTime = cancellation collectFirst {
        case OrderTrackingEvent.Cancel(t, _, Some(AbsoluteQuantity(q))) =>
          (fullOrder.fullQuantity - (q * fullOrder.fullQuantity.signum), t)
      }

      val impliedTradeQuantityFromObservationWithTime: Option[(BigDecimal, Instant)] =
        latestPresentObservationChange.map(c => (c.order.get.filledQuantity, c.timestamp))

      val impliedTradeQuantityWithTime: Option[(BigDecimal, Instant)] =
        (impliedTradeQuantityFromCancelWithTime, impliedTradeQuantityFromObservationWithTime) match {
          case (None, None) => None
          case (Some(x), None) => Some(x)
          case (None, Some(y)) => Some(y)
          case (Some(x), Some(y)) if x._1.abs == y._1.abs && x._2.isBefore(y._2) => Some(x)
          case (Some(x), Some(y)) if x._1.abs > y._1.abs => Some(x)
          case (Some(_), Some(y)) => Some(y)
        }

      impliedTradeQuantityWithTime match {
        case Some((q, t)) if q.abs > totalTradeQuantity.abs =>
          Some(ExpectingTrades(t, q - totalTradeQuantity))
        case _ => None
      }

    }

    val optionalExpectingObservationChange =
      if (isCurrentlyObserved && totalTradeQuantity.abs > filledQuantity.abs) {
        val expectedOrder = fullOrder.reduceQuantity(totalTradeQuantity)
        val optOrder = if (expectedOrder.openQuantity.abs > BigDecimal(0)) Some(expectedOrder) else None
        Some(ExpectingObservationChange(tradeEvents.last.timestamp, optOrder))
      }
      else if (isCurrentlyObserved && cancellation.isDefined) {
        Some(ExpectingObservationChange(cancellation.get.timestamp, None))
      }
      else None

    val optionalUnknownWhyGone =
      if (!isCurrentlyObserved && totalTradeQuantity != fullOrder.fullQuantity && cancellation.isEmpty)
        Some(UnknownWhyOrderIsGone(observationChanges.last.timestamp))
      else None

    val optionalUnknownIfMoreTradesBeforeCancel = {
      cancellation collectFirst {
        case OrderTrackingEvent.Cancel(timestamp, _, None) => UnknownIfMoreTradesBeforeCancel(timestamp)
      }
    }

    (optionalUnknownWhyGone
      ++ optionalExpectingTrades
      ++ optionalExpectingObservationChange
      ++ optionalUnknownIfMoreTradesBeforeCancel
      ).toSet
  }

}
