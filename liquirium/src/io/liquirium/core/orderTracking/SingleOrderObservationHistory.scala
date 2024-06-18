package io.liquirium.core.orderTracking

import io.liquirium.core.orderTracking.OrderTrackingEvent.ObservationChange

case class SingleOrderObservationHistory(changes: Seq[ObservationChange]) {

  def append(observationChange: ObservationChange): SingleOrderObservationHistory = {
    if (changes.nonEmpty && changes.last.order == observationChange.order) this else copy(changes :+ observationChange)
  }

}
