package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.orderTracking.OrderTrackingEvent.ObservationChange

case class SingleOrderObservationHistory(changes: Seq[ObservationChange]) {

  if (changes.isEmpty) {
    val m = "The observation history for a single order must contain at least one observation change"
    throw new RuntimeException(m)
  }

  def append(observationChange: ObservationChange): SingleOrderObservationHistory = {
    if (changes.last.order == observationChange.order) this else copy(changes :+ observationChange)
  }

  val latestPresentObservation: Option[Order] = changes.filter(_.order.isDefined).lastOption.flatMap(_.order)

  def hasDisappeared: Boolean = changes.last.order.isEmpty && latestPresentObservation.isDefined

}
