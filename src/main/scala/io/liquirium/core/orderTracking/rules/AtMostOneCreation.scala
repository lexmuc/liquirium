package io.liquirium.core.orderTracking.rules

import io.liquirium.core.orderTracking.BasicOrderTrackingState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.InconsistentEvents

object AtMostOneCreation extends ConsistencyRule {

  def check(state: BasicOrderTrackingState): Option[ErrorState] = {
    if (state.creationEvents.size > 1)
      Some(InconsistentEvents(state.creationEvents.init.last, state.creationEvents.last))
    else None
  }

}
