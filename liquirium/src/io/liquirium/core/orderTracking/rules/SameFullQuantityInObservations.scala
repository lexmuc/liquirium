package io.liquirium.core.orderTracking.rules

import io.liquirium.core.orderTracking.BasicOrderTrackingState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.InconsistentEvents
import io.liquirium.core.orderTracking.OrderTrackingEvent.ObservationChange

object SameFullQuantityInObservations extends ConsistencyRule {

  def check(state: BasicOrderTrackingState): Option[ErrorState] = {
    val reverseObservationChanges =
      state.observationEvents.collect { case o: ObservationChange => o}.reverse.toList

    for {
      lastObservation <- reverseObservationChanges.headOption
      mismatchingObservation <- reverseObservationChanges.tail.find(
        _.order.fullQuantity != lastObservation.order.fullQuantity
      )
    } yield InconsistentEvents(mismatchingObservation, lastObservation)
  }

}
