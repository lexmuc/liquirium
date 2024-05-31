package io.liquirium.core.orderTracking.rules

import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.InconsistentEvents
import io.liquirium.core.orderTracking.{BasicOrderTrackingState, OrderTrackingEvent}
import io.liquirium.util.AbsoluteQuantity

object CancelsAreConsistentWithOtherEvents extends ConsistencyRule {

  override def check(state: BasicOrderTrackingState): Option[ErrorState] = {
    if (state.cancellationEvents.size > 1) {
      Some(InconsistentEvents(state.cancellationEvents.init.last, state.cancellationEvents.last))
    }
    else state.cancellation match {
      case Some(cancel@OrderTrackingEvent.Cancel(_, _, Some(AbsoluteQuantity(absoluteRest)))) =>
        val conflictingObservation = state.observationChanges.reverseIterator collectFirst {
          case e@OrderTrackingEvent.ObservationChange(_, Some(o)) if o.fullQuantity.abs < absoluteRest => e
        }
        val conflictingCreation = state.creation collectFirst {
          case e@OrderTrackingEvent.Creation(_, o) if o.fullQuantity.abs < absoluteRest => e
        }
        val conflictingEvent = conflictingObservation orElse conflictingCreation
        conflictingEvent map { e => InconsistentEvents(cancel, e) }
      case _ => None
    }
  }

}
