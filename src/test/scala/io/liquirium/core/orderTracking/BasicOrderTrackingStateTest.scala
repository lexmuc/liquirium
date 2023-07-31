package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.{BasicTest, OrderHelpers, TradeHelpers}
import io.liquirium.core.orderTracking.BasicOrderTrackingState.{ErrorState, SyncReason}
import io.liquirium.util.AbsoluteQuantity

import java.time.Instant

class BasicOrderTrackingStateTest extends BasicTest {

  protected val orderId = "oid"

  protected def o(n: Int, of: Int): Order = OrderHelpers.order(orderId, quantity = dec(n), originalQuantity = dec(of))

  protected var operationEvents: Seq[OrderTrackingEvent.OperationEvent] = Seq()
  protected var observationChanges: Seq[OrderTrackingEvent.ObservationChange] = Seq()
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

  protected def change(t: Instant, order: Order): OrderTrackingEvent.ObservationChange =
    OrderTrackingEvent.ObservationChange(t, Some(order))

  protected def absence(t: Instant): OrderTrackingEvent.ObservationChange =
    OrderTrackingEvent.ObservationChange(t, None)

  protected def trade(t: Instant, quantity: Int): OrderTrackingEvent.NewTrade = {
    val trade = TradeHelpers.trade(orderId = Some(orderId), quantity = dec(quantity), time = t)
    OrderTrackingEvent.NewTrade(trade)
  }

  protected def observe(ee: OrderTrackingEvent*): Seq[OrderTrackingEvent] = {
    ee.foreach {
      case c: OrderTrackingEvent.Creation => operationEvents = operationEvents :+ c
      case c: OrderTrackingEvent.Cancel => operationEvents = operationEvents :+ c
      case o: OrderTrackingEvent.ObservationChange => observationChanges = observationChanges :+ o
      case t: OrderTrackingEvent.NewTrade => tradeEvents = tradeEvents :+ t
    }
    ee
  }

  protected def state: BasicOrderTrackingState =
    BasicOrderTrackingState(
      operationEvents = operationEvents,
      observationHistory = SingleOrderObservationHistory(observationChanges),
      tradeEvents = tradeEvents,
    )

  protected def assertIsCurrentlyNotObserved(): Unit = state.isCurrentlyObserved shouldBe false

  protected def assertIsCurrentlyObserved(): Unit = state.isCurrentlyObserved shouldBe true

  protected def assertReportedState(s: Order): Unit = state.reportingState shouldEqual Some(s)

  protected def assertNotReported(): Unit = state.reportingState shouldEqual None

  protected def assertInSync(): Unit = {
    state.syncReasons shouldEqual Set()
    state.errorState shouldEqual None
  }

  protected def assertSyncReasons(reasons: SyncReason*): Unit = {
    state.syncReasons shouldEqual reasons.toSet
    state.errorState shouldEqual None
  }

  protected def unknownWhyOrderIsGone(time: Instant): SyncReason = SyncReason.UnknownWhyOrderIsGone(time)

  protected def expectingObservationChange(since: Instant, observation: Option[Order]): SyncReason =
    SyncReason.ExpectingObservationChange(since, observation)

  protected def expectingTrades(time: Instant, quantity: BigDecimal): SyncReason =
    SyncReason.ExpectingTrades(time: Instant, quantity: BigDecimal)

  protected def unknownIfMoreTradesBeforeCancel(time: Instant): SyncReason =
    SyncReason.UnknownIfMoreTradesBeforeCancel(time: Instant)

  protected def assertInconsistentEvents(eventA: OrderTrackingEvent, eventB: OrderTrackingEvent): Unit = {
    state.errorState shouldEqual Some(ErrorState.InconsistentEvents(eventA, eventB))
  }

  protected def assertOverfill(event: OrderTrackingEvent, totalFill: BigDecimal, maxFill: BigDecimal): Unit =
    state.errorState shouldEqual Some(
      ErrorState.Overfill(
        event.asInstanceOf[OrderTrackingEvent.NewTrade],
        totalFill = totalFill,
        maxFill = maxFill,
      )
    )

  protected def assertReappearingOrderInconsistency(event: OrderTrackingEvent): Unit = {
    state.errorState shouldEqual Some(ErrorState.ReappearingOrderInconsistency(event))
  }

}
