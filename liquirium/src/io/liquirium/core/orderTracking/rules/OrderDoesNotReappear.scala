package io.liquirium.core.orderTracking.rules

import io.liquirium.core.orderTracking.BasicOrderTrackingState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.ReappearingOrderInconsistency

object OrderDoesNotReappear extends ConsistencyRule {

  override def check(state: BasicOrderTrackingState): Option[ErrorState] = {
    val eventsAfterSeeingTheOrder =
      state.observationChanges.dropWhile(_.order.isEmpty).dropWhile(_.order.isDefined)
    eventsAfterSeeingTheOrder.find(_.order.isDefined).map { surprisingAppearance =>
      ReappearingOrderInconsistency(surprisingAppearance)
    }
  }

}
