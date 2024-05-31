package io.liquirium.core.orderTracking.rules

import io.liquirium.core.orderTracking.BasicOrderTrackingState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.InconsistentEvents

object SameFullQuantityInObservations extends ConsistencyRule {

  def check(state: BasicOrderTrackingState): Option[ErrorState] = {
    val reverseNonEmptyObservations = state.observationChanges.filter(_.order.isDefined).reverse.toList

    for {
      lastObservation <- reverseNonEmptyObservations.headOption
      mismatchingObservation <- reverseNonEmptyObservations.tail.find(
        _.order.get.fullQuantity != lastObservation.order.get.fullQuantity
      )
    } yield InconsistentEvents(mismatchingObservation, lastObservation)

  }

}
