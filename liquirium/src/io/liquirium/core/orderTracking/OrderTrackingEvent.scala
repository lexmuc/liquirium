package io.liquirium.core.orderTracking

import io.liquirium.core.{Order, Trade}
import io.liquirium.util.AbsoluteQuantity

import java.time.Instant

sealed trait OrderTrackingEvent {
  def timestamp: Instant
  def orderId: String
}

object OrderTrackingEvent {

  case class NewTrade(t: Trade) extends OrderTrackingEvent {
    if (t.orderId.isEmpty) {
      throw new IllegalArgumentException("Trade must have an orderId")
    }

    override def timestamp: Instant = t.time

    override def orderId: String = t.orderId.get
  }

  sealed trait OrderObservationEvent extends OrderTrackingEvent {
    def maybeOrder: Option[Order]
  }

  case class ObservationChange(timestamp: Instant, order: Order) extends OrderObservationEvent {
    override def orderId: String = order.id

    override def maybeOrder: Option[Order] = Some(order)
  }

  case class Disappearance(timestamp: Instant, orderId: String) extends OrderObservationEvent {
    override def maybeOrder: Option[Order] = None
  }

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
