package io.liquirium.core.orderTracking.helpers

import io.liquirium.core.Order
import io.liquirium.core.orderTracking.BasicOrderTrackingState.{ErrorState, SyncReason}
import io.liquirium.core.orderTracking.OrderTrackingEvent
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant

class BasicOrderTrackingStateTest extends SingleOrderTrackingTest {

  protected def assertIsCurrentlyNotObserved(): Unit = basicState.isCurrentlyObserved shouldBe false

  protected def assertIsCurrentlyObserved(): Unit = basicState.isCurrentlyObserved shouldBe true

  protected def assertReportedState(s: Order): Unit = basicState.reportingState shouldEqual Some(s)

  protected def assertNotReported(): Unit = basicState.reportingState shouldEqual None

  protected def assertInSync(): Unit = {
    basicState.syncReasons shouldEqual Set()
    basicState.errorState shouldEqual None
  }

  protected def assertSyncReasons(reasons: SyncReason*): Unit = {
    basicState.syncReasons shouldEqual reasons.toSet
    basicState.errorState shouldEqual None
  }

  protected def unknownWhyOrderIsGone(time: Instant): SyncReason = SyncReason.UnknownWhyOrderIsGone(time)

  protected def expectingObservationChange(since: Instant, observation: Option[Order]): SyncReason =
    SyncReason.ExpectingObservationChange(since, observation)

  protected def expectingOrderToAppear(since: Instant, order: Order): SyncReason =
    SyncReason.ExpectingOrderToAppear(since, order)

  protected def expectingTrades(time: Instant, quantity: BigDecimal): SyncReason =
    SyncReason.ExpectingTrades(time: Instant, quantity: BigDecimal)

  protected def unknownIfMoreTradesBeforeCancel(time: Instant): SyncReason =
    SyncReason.UnknownIfMoreTradesBeforeCancel(time: Instant)

  protected def assertInconsistentEvents(eventA: OrderTrackingEvent, eventB: OrderTrackingEvent): Unit = {
    basicState.errorState shouldEqual Some(ErrorState.InconsistentEvents(eventA, eventB))
  }

  protected def assertOverfill(event: OrderTrackingEvent, totalFill: BigDecimal, maxFill: BigDecimal): Unit =
    basicState.errorState shouldEqual Some(
      ErrorState.Overfill(
        event.asInstanceOf[OrderTrackingEvent.NewTrade],
        totalFill = totalFill,
        maxFill = maxFill,
      )
    )

  protected def assertReappearingOrderInconsistency(event: OrderTrackingEvent): Unit = {
    basicState.errorState shouldEqual Some(ErrorState.ReappearingOrderInconsistency(event))
  }

}
