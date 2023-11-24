package io.liquirium.core.orderTracking

import io.liquirium.core.{Order, Trade}
import io.liquirium.util.AbsoluteQuantity

import java.time.Instant

sealed trait OrderTrackingEvent {
  def timestamp: Instant
}

object OrderTrackingEvent {

  case class NewTrade(t: Trade) extends OrderTrackingEvent {
    override def timestamp: Instant = t.time
  }

  case class ObservationChange(timestamp: Instant, order: Option[Order]) extends OrderTrackingEvent

  trait OperationEvent extends OrderTrackingEvent {
    def orderId: String
  }

  case class Creation(timestamp: Instant, order: Order) extends OperationEvent {
    override def orderId: String = order.id
  }

  case class Cancel(
    timestamp: Instant,
    orderId: String,
    absoluteRestQuantity: Option[AbsoluteQuantity],
  ) extends OperationEvent

}
