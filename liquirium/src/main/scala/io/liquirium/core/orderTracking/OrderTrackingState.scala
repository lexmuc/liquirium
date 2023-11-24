package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.orderTracking.OrderTrackingState.ReportingState

trait OrderTrackingState {
  def reportingState: ReportingState
}

object OrderTrackingState {

  sealed trait ReportingState

  // There may still be a legacy version of the reporting state!
  object ReportingState {
    case class Reportable(o: Order) extends ReportingState
    case object Syncing extends ReportingState
    case object Completed extends ReportingState
  }

  def apply(rs: ReportingState): OrderTrackingState = Impl(rs)

  private case class Impl(rs: ReportingState) extends OrderTrackingState {

    def reportingState: ReportingState = rs

  }

}
