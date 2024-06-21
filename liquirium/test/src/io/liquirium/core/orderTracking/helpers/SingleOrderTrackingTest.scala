package io.liquirium.core.orderTracking.helpers

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.{BasicTest, OrderHelpers, TradeHelpers}
import io.liquirium.core.orderTracking.OrderTrackingEvent.{Disappearance, ObservationChange}
import io.liquirium.core.orderTracking.{BasicOrderTrackingState, OrderTrackingEvent}
import io.liquirium.util.AbsoluteQuantity

import java.time.Instant

class SingleOrderTrackingTest extends BasicTest {

  protected val orderId = "oid"

  protected def o(n: Int, of: Int): Order = OrderHelpers.order(orderId, quantity = dec(n), originalQuantity = dec(of))

  protected var operationEvents: Seq[OrderTrackingEvent.OperationEvent] = Seq()
  protected var observationChanges: Seq[OrderTrackingEvent.OrderObservationEvent] = Seq()
  protected var tradeEvents: Seq[OrderTrackingEvent.NewTrade] = Seq()

  protected def resetEvents(): Unit = {
    operationEvents = Seq()
    observationChanges = Seq()
    tradeEvents = Seq()
  }

  protected def creation(t: Instant, order: Order): OrderTrackingEvent.Creation =
    OrderTrackingEvent.Creation(t, order)

  protected def cancellation(t: Instant, absoluteRest: Option[Int] = None): OrderTrackingEvent.Cancel =
    OrderTrackingEvent.Cancel(t, orderId, absoluteRest.map(x => AbsoluteQuantity(dec(x))))

  protected def change(t: Instant, order: Order): OrderTrackingEvent.OrderObservationEvent =
    ObservationChange(t, order)

  protected def disappearance(t: Instant): OrderTrackingEvent.OrderObservationEvent =
    Disappearance(t, orderId)

  protected def trade(t: Instant, quantity: Int): OrderTrackingEvent.NewTrade = {
    val trade = TradeHelpers.trade(orderId = Some(orderId), quantity = dec(quantity), time = t)
    OrderTrackingEvent.NewTrade(trade)
  }

  protected def observe(ee: OrderTrackingEvent*): Seq[OrderTrackingEvent] = {
    ee.foreach {
      case c: OrderTrackingEvent.Creation => operationEvents = operationEvents :+ c
      case c: OrderTrackingEvent.Cancel => operationEvents = operationEvents :+ c
      case o: OrderTrackingEvent.OrderObservationEvent => observationChanges = observationChanges :+ o
      case t: OrderTrackingEvent.NewTrade => tradeEvents = tradeEvents :+ t
    }
    ee
  }

  protected def basicState: BasicOrderTrackingState =
    BasicOrderTrackingState(
      operationEvents = operationEvents,
      tradeEvents = tradeEvents,
      observationEvents = observationChanges,
    )

}
