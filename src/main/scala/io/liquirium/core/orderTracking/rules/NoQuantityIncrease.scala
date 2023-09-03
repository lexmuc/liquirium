package io.liquirium.core.orderTracking.rules

import io.liquirium.core.Order
import io.liquirium.core.orderTracking.BasicOrderTrackingState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.InconsistentEvents
import io.liquirium.core.orderTracking.OrderTrackingEvent.ObservationChange

import scala.annotation.tailrec

object NoQuantityIncrease extends ConsistencyRule {

  private def isConsistent(oldState: Order, newState: Order): Boolean =
    oldState.openQuantity.abs >= newState.openQuantity.abs

  @tailrec
  private def findInconsistentObservations(
    reverseNonEmptyObservations: List[ObservationChange],
  ): Option[InconsistentEvents] =
    if (reverseNonEmptyObservations.isEmpty || reverseNonEmptyObservations.tail.isEmpty) None
    else {
      val lastEvent = reverseNonEmptyObservations.head
      reverseNonEmptyObservations.tail.find(obs => !isConsistent(obs.order.get, lastEvent.order.get)) match {
        case Some(e) => Some(InconsistentEvents(e, lastEvent))
        case None => findInconsistentObservations(reverseNonEmptyObservations.tail)
      }
    }

  override def check(state: BasicOrderTrackingState): Option[ErrorState] = {
    val reverseNonEmptyObservations = state.observationHistory.changes.filter(_.order.isDefined).reverse.toList
    findInconsistentObservations(reverseNonEmptyObservations)
  }

}
