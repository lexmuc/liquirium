package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState._
import io.liquirium.core.orderTracking.BasicOrderTrackingState.SyncReason._
import io.liquirium.core.orderTracking.BasicOrderTrackingState._
import io.liquirium.core.orderTracking.OrderTrackingEvent.ObservationChange
import io.liquirium.util.AbsoluteQuantity

import java.time.Instant
import scala.annotation.tailrec


object BasicOrderTrackingState {

  sealed trait SyncReason

  sealed trait ErrorState

  object SyncReason {

    case class UnknownWhyOrderIsGone(time: Instant) extends SyncReason

    case class ExpectingTrades(time: Instant, quantity: BigDecimal) extends SyncReason

    case class ExpectingObservationChange(since: Instant, observation: Option[Order]) extends SyncReason

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
  observationHistory: SingleOrderObservationHistory,
  tradeEvents: Seq[OrderTrackingEvent.NewTrade],
) {

  private val creationEvents = operationEvents.collect { case c: OrderTrackingEvent.Creation => c }
  private val creation: Option[OrderTrackingEvent.Creation] = creationEvents.headOption

  private val cancellationEvents = operationEvents.collect { case c: OrderTrackingEvent.Cancel => c }
  private val cancellation: Option[OrderTrackingEvent.Cancel] = cancellationEvents.headOption

  private val totalTradeQuantity: BigDecimal = tradeEvents.map(_.t.quantity).sum

  private val orderWithFullQuantity: Option[Order] =
    observationHistory.latestPresentObservation.map(_.resetQuantity) orElse creation.map(_.order)

  val errorState: Option[ErrorState] =
    findInconsistentObservations(observationHistory.changes.filter(_.order.isDefined).reverse.toList) orElse
      findInconsistentCreations() orElse
      findInconsistentCancels() orElse
      findReappearingOrder(observationHistory.changes) orElse
      findOverfill()

  private val lastObservationHistoryTime = observationHistory.changes.last.timestamp

  val isCurrentlyObserved: Boolean = observationHistory.changes.last.order.isDefined

  val syncReasons: Set[SyncReason] = orderWithFullQuantity match {
    case None => syncReasonsIfNeverObserved()
    case Some(o) => syncReasonsIfObserved(o)
  }

  val reportingState: Option[Order] =
    if (!isCurrentlyObserved || cancellation.isDefined) None
    else observationHistory.latestPresentObservation match {
      case Some(lastObservedOrder) if totalTradeQuantity.abs <= lastObservedOrder.fullQuantity.abs =>
        lastObservedOrder.resetQuantity.reduceQuantity(totalTradeQuantity)
      case _ => None
    }

  private def syncReasonsIfNeverObserved(): Set[SyncReason] =
    if (tradeEvents.nonEmpty && cancellation.isEmpty) {
      Set(UnknownWhyOrderIsGone(tradeEvents.last.timestamp))
    }
    else Set()

  private def syncReasonsIfObserved(fullOrder: Order): Set[SyncReason] = {
    val filledQuantity = observationHistory.changes.reverseIterator.map(_.order).collectFirst {
      case Some(o) => o.filledQuantity
    } getOrElse BigDecimal(0)

    val optionalExpectingTrades: Option[ExpectingTrades] = {
      val impliedTradeQuantityFromCancelWithTime = cancellation collectFirst {
        case OrderTrackingEvent.Cancel(t, _, Some(AbsoluteQuantity(q))) =>
          (fullOrder.fullQuantity - (q * fullOrder.fullQuantity.signum), t)
      }

      val impliedTradeQuantityFromObservationWithTime: Option[(BigDecimal, Instant)] =
        observationHistory.changes.reverseIterator.collectFirst {
          case OrderTrackingEvent.ObservationChange(t, Some(o)) => (o.filledQuantity, t)
        }

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
        Some(ExpectingObservationChange(tradeEvents.last.timestamp, expectedOrder))
      }
      else if (isCurrentlyObserved && cancellation.isDefined) {
        Some(ExpectingObservationChange(cancellation.get.timestamp, None))
      }
      else None

    val optionalUnknownWhyGone =
      if (!isCurrentlyObserved && totalTradeQuantity != fullOrder.fullQuantity && cancellation.isEmpty)
        Some(UnknownWhyOrderIsGone(lastObservationHistoryTime))
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

  private def findReappearingOrder(observations: Seq[ObservationChange]): Option[ReappearingOrderInconsistency] = {
    val eventsAfterSeeingTheOrder = observations.dropWhile(_.order.isEmpty).dropWhile(_.order.isDefined)
    eventsAfterSeeingTheOrder.find(_.order.isDefined).map { surprisingAppearance =>
      ReappearingOrderInconsistency(surprisingAppearance)
    }
  }

  private def findOverfill(): Option[Overfill] =
    if (orderWithFullQuantity.isDefined) {
      val fullOrderQuantity = orderWithFullQuantity.get.fullQuantity
      cancellation match {
        case Some(OrderTrackingEvent.Cancel(_, _, Some(AbsoluteQuantity(q))))
          if totalTradeQuantity.abs > fullOrderQuantity.abs - q =>
          val maxFill =
            if (fullOrderQuantity.signum > 0) fullOrderQuantity - q
            else fullOrderQuantity + q
          Some(Overfill(tradeEvents.last, totalFill = totalTradeQuantity, maxFill = maxFill))
        case _ if totalTradeQuantity.abs > fullOrderQuantity.abs =>
          Some(Overfill(tradeEvents.last, totalFill = totalTradeQuantity, maxFill = fullOrderQuantity))
        case _ =>
          None
      }
    }
    else None

  @tailrec
  private def findInconsistentObservations(
    reverseNonEmptyObservations: List[ObservationChange],
  ): Option[InconsistentEvents] = {
    def isConsistent(oldState: Order, newState: Order): Boolean =
      oldState.fullQuantity == newState.fullQuantity && oldState.openQuantity.abs >= newState.openQuantity.abs

    if (reverseNonEmptyObservations.isEmpty || reverseNonEmptyObservations.tail.isEmpty) None
    else {
      val lastEvent = reverseNonEmptyObservations.head
      reverseNonEmptyObservations.tail.find(obs => !isConsistent(obs.order.get, lastEvent.order.get)) match {
        case Some(e) => Some(InconsistentEvents(e, lastEvent))
        case None => findInconsistentObservations(reverseNonEmptyObservations.tail)
      }
    }
  }

  private def findInconsistentCreations(): Option[InconsistentEvents] = {
    val lastObservationEvent = observationHistory.changes.reverseIterator.find(_.order.isDefined)
    if (creationEvents.size > 1) Some(InconsistentEvents(creationEvents.init.last, creationEvents.last))
    else if (creation.isDefined && lastObservationEvent.isDefined) {
      if (creation.get.order.fullQuantity != observationHistory.latestPresentObservation.get.fullQuantity) {
        Some(InconsistentEvents(creation.get, lastObservationEvent.get))
      }
      else None
    }
    else None
  }

  private def findInconsistentCancels(): Option[InconsistentEvents] =
    if (cancellationEvents.size > 1) {
      Some(InconsistentEvents(cancellationEvents.init.last, cancellationEvents.last))
    }
    else cancellation match {
      case Some(cancel@OrderTrackingEvent.Cancel(_, _, Some(AbsoluteQuantity(absoluteRest)))) =>
        val conflictingObservation = observationHistory.changes.reverseIterator collectFirst {
          case e@OrderTrackingEvent.ObservationChange(_, Some(o)) if o.fullQuantity.abs < absoluteRest => e
        }
        val conflictingCreation = creation collectFirst {
          case e@OrderTrackingEvent.Creation(_, o) if o.fullQuantity.abs < absoluteRest => e
        }
        val conflictingEvent = conflictingObservation orElse conflictingCreation
        conflictingEvent map { e => InconsistentEvents(cancel, e) }
      case _ => None
    }

}
