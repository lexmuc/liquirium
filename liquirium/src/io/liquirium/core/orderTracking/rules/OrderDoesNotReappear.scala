package io.liquirium.core.orderTracking.rules

import io.liquirium.core.orderTracking.BasicOrderTrackingState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.ReappearingOrderInconsistency

object OrderDoesNotReappear extends ConsistencyRule {

  override def check(state: BasicOrderTrackingState): Option[ErrorState] = {
    val eventsAfterSeeingTheOrder =
      state.observationEvents.dropWhile(_.maybeOrder.isEmpty).dropWhile(_.maybeOrder.isDefined)
    eventsAfterSeeingTheOrder.find(_.maybeOrder.isDefined).map { surprisingAppearance =>
      ReappearingOrderInconsistency(surprisingAppearance)
    }
  }

}
