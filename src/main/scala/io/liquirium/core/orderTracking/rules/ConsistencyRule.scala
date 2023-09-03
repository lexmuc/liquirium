package io.liquirium.core.orderTracking.rules

import io.liquirium.core.orderTracking.BasicOrderTrackingState
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState

trait ConsistencyRule {

  def check(state: BasicOrderTrackingState): Option[ErrorState]

}
