package io.liquirium.core.orderTracking

import io.liquirium.core.orderTracking.OrderTrackingEvent.OrderObservationEvent

case class SingleOrderObservationHistory(changes: Seq[OrderObservationEvent]) {

  def append(observationChange: OrderObservationEvent): SingleOrderObservationHistory = {
    if (changes.nonEmpty && changes.last.maybeOrder == observationChange.maybeOrder) this
    else copy(changes :+ observationChange)
  }

}
