package io.liquirium.core.orderTracking.helpers

import io.liquirium.core.helpers.CoreHelpers.{dec, millis, sec}
import io.liquirium.core.helpers.{OrderHelpers, TradeHelpers}
import io.liquirium.core.orderTracking.BasicOrderTrackingState.ErrorState.ReappearingOrderInconsistency
import io.liquirium.core.orderTracking.BasicOrderTrackingState.SyncReason
import io.liquirium.core.orderTracking.OrderTrackingEvent.{Disappearance, NewTrade, ObservationChange, OrderObservationEvent}
import io.liquirium.core.orderTracking._
import io.liquirium.core.{Order, OrderSet, orderTracking}
import io.liquirium.util.AbsoluteQuantity
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant

object OrderTrackingHelpers {

  def openOrdersSnapshot(time: Instant, orders: Order*): OpenOrdersSnapshot =
    orderTracking.OpenOrdersSnapshot(OrderSet(orders.toSet), time)

  def openOrdersSnapshot(orders: OrderSet, time: Instant): OpenOrdersSnapshot =
    orderTracking.OpenOrdersSnapshot(orders, time)

  def openOrdersHistory(snapshots: OpenOrdersSnapshot*): OpenOrdersHistory = {
    snapshots.tail.foldLeft(OpenOrdersHistory.start(snapshots.head)) { case (h, s) => h.appendIfChanged(s) }
  }

  def basicOrderTrackingState(
    orderObservationChanges: Seq[OrderTrackingEvent.OrderObservationEvent],
    operationEvents: Seq[OrderTrackingEvent.OperationEvent] = Seq(),
    tradeEvents: Seq[OrderTrackingEvent.NewTrade] = Seq(),
  ): BasicOrderTrackingState =
    BasicOrderTrackingState(
      operationEvents = operationEvents,
      tradeEvents = tradeEvents,
      observationEvents = orderObservationChanges,
    )

  def singleOrderObservationHistory(changes: OrderObservationEvent*): SingleOrderObservationHistory =
    SingleOrderObservationHistory(changes)

  def disappearance(t: Instant, orderId: String): OrderObservationEvent = Disappearance(t, orderId)

  def observationChange(t: Instant, order: Order): OrderObservationEvent = ObservationChange(t, order)

  def tradeEvent(
    n: Int,
    time: Instant,
    orderId: String,
    quantity: BigDecimal,
  ): NewTrade = NewTrade(TradeHelpers.trade(
    id = n.toString,
    orderId = Some(orderId),
    quantity = quantity,
    time = time,
  ))

  def cancelEvent(
    t: Instant,
    orderId: String,
    absoluteRestQuantity: Option[Integer] = None,
  ): OrderTrackingEvent.Cancel =
    OrderTrackingEvent.Cancel(
      timestamp = t,
      orderId = orderId,
      absoluteRestQuantity = absoluteRestQuantity.map(q =>AbsoluteQuantity(dec(q))),
    )

  def creationEvent(
    t: Instant,
    order: Order,
  ): OrderTrackingEvent.Creation =
    OrderTrackingEvent.Creation(
      timestamp = t,
      order = order,
    )

  def syncedStateWithReportableOrder(o: Order): BasicOrderTrackingState = {
    val result = basicOrderTrackingState(
      orderObservationChanges = Seq(observationChange(sec(0), o)),
    )
    result.errorState shouldBe None
    result.syncReasons.size shouldBe 0
    result.reportingState shouldEqual Some(o)
    result
  }

  def syncedStateWithoutReportableOrder(orderId: String): BasicOrderTrackingState = {
    val order = OrderHelpers.order(id = orderId, quantity = 1)
    val result = basicOrderTrackingState(
      orderObservationChanges = Seq(
        observationChange(sec(0), order),
        disappearance(sec(1), orderId),
      ),
      operationEvents = Seq(
        cancelEvent(sec(1), order.id, absoluteRestQuantity = Some(1)),
      ),
    )
    result.errorState shouldBe None
    result.syncReasons.size shouldBe 0
    result.reportingState.isDefined shouldBe false
    result
  }

  def stateWithSyncReasonUnknownWhyGone(order: Order, t: Instant): BasicOrderTrackingState = {
    val result = basicOrderTrackingState(
      orderObservationChanges = Seq(
        observationChange(sec(0), order),
        disappearance(t, order.id),
      ),
    )
    result.errorState shouldBe None
    result.syncReasons shouldEqual Set(SyncReason.UnknownWhyOrderIsGone(t))
    result.reportingState.isDefined shouldBe false
    result
  }

  def stateWithSyncReasonExpectingTrades(
    orderId: String,
    t: Instant,
    expectedTradeQuantity: BigDecimal,
  ): BasicOrderTrackingState = {
    val fullQuantity = expectedTradeQuantity + dec(1)
    val fullOrder = OrderHelpers.order(
      id = orderId,
      quantity = fullQuantity,
      originalQuantity = fullQuantity,
    )
    val result = basicOrderTrackingState(
      orderObservationChanges = Seq(
        observationChange(t minus millis(1), fullOrder),
        observationChange(t, fullOrder.copy(openQuantity = fullQuantity - expectedTradeQuantity)),
      ),
    )
    result.errorState shouldBe None
    result.syncReasons shouldEqual Set(SyncReason.ExpectingTrades(t, expectedTradeQuantity))
    result.reportingState.isDefined shouldBe true
    result
  }

  def stateWithSyncReasonExpectingObservationChange(
    order: Order,
    t: Instant,
  ): BasicOrderTrackingState = {
    val result = basicOrderTrackingState(
      orderObservationChanges = Seq(
        observationChange(t minus millis(1), order),
      ),
      tradeEvents = Seq(
        tradeEvent(123, orderId = order.id, quantity = order.openQuantity, time = t),
      ),
    )
    result.errorState shouldBe None
    result.syncReasons shouldEqual Set(SyncReason.ExpectingObservationChange(t, None))
    result.reportingState.isDefined shouldBe false
    result
  }

  def stateWithReappearingOrderError(order: Order, t: Instant): BasicOrderTrackingState = {
    val result = basicOrderTrackingState(
      orderObservationChanges = Seq(
        observationChange(t minus millis(2), order),
        disappearance(t minus millis(1), order.id),
        observationChange(t, order),
      ),
    )
    result.errorState shouldEqual Some(ReappearingOrderInconsistency(observationChange(t, order)))
    result.syncReasons.size shouldBe 0
    result
  }

  def stateWithSyncReasonUnknownIfMoreTrades(
    order: Order,
    t: Instant,
  ): BasicOrderTrackingState = {
    val result = basicOrderTrackingState(
      orderObservationChanges = Seq(
        observationChange(t minus millis(1), order),
        disappearance(t plus millis(1), order.id),
      ),
      operationEvents = Seq(
        cancelEvent(t, order.id),
      ),
    )
    result.errorState shouldBe None
    result.syncReasons shouldEqual Set(SyncReason.UnknownIfMoreTradesBeforeCancel(t))
    result.reportingState shouldBe None
    result
  }

  def stateWithSyncReasonExpectingOrderToAppear(
    order: Order,
    t: Instant,
  ): BasicOrderTrackingState = {
    val result = basicOrderTrackingState(
      orderObservationChanges = Seq(),
      operationEvents = Seq(
        creationEvent(t, order),
      ),
    )
    result.errorState shouldBe None
    result.syncReasons shouldEqual Set(SyncReason.ExpectingOrderToAppear(t, order))
    result.reportingState shouldBe None
    result
  }

}
