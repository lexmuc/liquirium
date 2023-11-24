package io.liquirium.core.orderTracking.rules

import io.liquirium.core.orderTracking.BasicOrderTrackingState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.InconsistentEvents
import io.liquirium.core.orderTracking.OrderTrackingEvent.ObservationChange

object FullQuantityAtCreationMatchesTheLatestPresentObservation extends ConsistencyRule {

  def check(state: BasicOrderTrackingState): Option[ErrorState] =
    (state.creation, state.observationHistory.latestPresentChange) match {
      case (Some(c), Some(obs@ObservationChange(_, Some(o)) )) if c.order.fullQuantity != o.fullQuantity =>
        Some(InconsistentEvents(c, obs))
      case _ => None
    }

}
