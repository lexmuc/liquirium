package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.orderTracking.helpers.BasicOrderTrackingStateTest

class BasicOrderTrackingStateTest_4_WithCancel extends BasicOrderTrackingStateTest {

  test("when only a cancel event is present, no order is reported") {
    observe(
      cancellation(sec(1)),
      disappearance(sec(1)),
    )
    assertNotReported()
  }

  test("when only a cancel event is present, it is syncing because it is unknown if trades still appear") {
    observe(
      cancellation(sec(1)),
      disappearance(sec(1)),
    )
    assertSyncReasons(unknownIfMoreTradesBeforeCancel(sec(1)))

    resetEvents()

    observe(
      cancellation(sec(1), absoluteRest = Some(10)),
      disappearance(sec(1)),
    )
    assertSyncReasons(unknownIfMoreTradesBeforeCancel(sec(1)))
  }

  test("when only a cancel event and trades are present, no order is reported") {
    observe(
      trade(sec(0), 1),
      cancellation(sec(1)),
      disappearance(sec(1)),
    )
    assertNotReported()
  }

  test("when only a cancel event and trades are present, it is syncing because it is unknown if trades still appear") {
    observe(
      trade(sec(0), 1),
      cancellation(sec(1)),
      disappearance(sec(1)),
    )
    assertSyncReasons(unknownIfMoreTradesBeforeCancel(sec(1)))

    resetEvents()

    observe(
      trade(sec(0), 1),
      cancellation(sec(1), absoluteRest = Some(10)),
      disappearance(sec(1)),
    )
    assertSyncReasons(unknownIfMoreTradesBeforeCancel(sec(1)))
  }

  test("when an order has been cancelled with known rest quantity and the order is gone sync status is in sync") {
    observe(
      change(sec(1), o(10, of = 10)), // immediately observed
      cancellation(sec(1), Some(10)),
      disappearance(sec(2)),
    )
    assertNotReported()
    assertInSync()

    resetEvents()

    observe(
      disappearance(sec(0)),
      change(sec(1), o(10, of = 10)), // not observed at first
      cancellation(sec(1), Some(10)),
      disappearance(sec(2)),
    )
    assertNotReported()
    assertInSync()
  }

  test("if the order is still there after a cancel it is expecting an observation change and order is reported gone") {
    observe(
      disappearance(sec(0)),
      cancellation(sec(1), Some(10)),
      change(sec(2), o(10, of = 10)),
    )
    assertNotReported()
    assertSyncReasons(expectingObservationChange(sec(1), None))
  }

  test("if the rest quantity implies more trades have happened, they are expected, but order is reported gone") {
    observe(
      change(sec(2), o(10, of = 10)),
      cancellation(sec(3), absoluteRest = Some(2)),
      disappearance(sec(4)),
    )
    assertNotReported()
    assertSyncReasons(expectingTrades(sec(3), 8))

    resetEvents()

    observe(
      change(sec(2), o(-10, of = -10)),
      cancellation(sec(3), absoluteRest = Some(2)),
      disappearance(sec(4)),
    )
    assertNotReported()
    assertSyncReasons(expectingTrades(sec(3), -8))
  }

  test("implied trades are expected regardless of whether order is gone") {
    observe(
      change(sec(2), o(10, of = 10)),
      cancellation(sec(3), absoluteRest = Some(2)),
    )
    assertSyncReasons(expectingTrades(sec(3), 8), expectingObservationChange(sec(3), None))

    observe(disappearance(sec(4)))
    assertSyncReasons(expectingTrades(sec(3), 8))
  }

  test("expected trades are reduced by the trades that already happened") {
    observe(
      change(sec(2), o(10, of = 10)),
      cancellation(sec(3), absoluteRest = Some(2)),
      trade(sec(3), 5),
      disappearance(sec(4)),
    )
    assertSyncReasons(expectingTrades(sec(3), 3))

    resetEvents()

    observe(
      change(sec(2), o(-10, of = -10)),
      cancellation(sec(3), absoluteRest = Some(2)),
      trade(sec(3), -5),
      disappearance(sec(4)),
    )
    assertSyncReasons(expectingTrades(sec(3), -3))
  }

  test("there are no expected trades when the cancel size matches size implied by trades") {
    observe(
      change(sec(2), o(10, of = 10)),
      cancellation(sec(3), absoluteRest = Some(2)),
      trade(sec(3), 8),
      disappearance(sec(4)),
    )
    assertInSync()

    resetEvents()

    observe(
      change(sec(2), o(-10, of = -10)),
      cancellation(sec(3), absoluteRest = Some(2)),
      trade(sec(3), -4),
      trade(sec(4), -4),
      disappearance(sec(4)),
    )
    assertInSync()
  }

  test("if fewer trades are also implied from last observation it expects the trades implied by the cancellation") {
    observe(
      change(sec(2), o(10, of = 10)),
      cancellation(sec(3), absoluteRest = Some(4)),
      change(sec(4), o(5, of = 10)),
      disappearance(sec(5)),
    )
    assertSyncReasons(expectingTrades(sec(3), 6))

    resetEvents()

    observe(
      change(sec(2), o(-10, of = -10)),
      cancellation(sec(3), absoluteRest = Some(4)),
      change(sec(4), o(-5, of = -10)),
      disappearance(sec(5)),
    )
    assertSyncReasons(expectingTrades(sec(3), -6))
  }

  test("if more trades are implied from the last observation it expects the trades implied by the observation") {
    observe(
      change(sec(2), o(10, of = 10)),
      cancellation(sec(3), absoluteRest = Some(9)),
      change(sec(4), o(5, of = 10)),
      disappearance(sec(5)),
    )
    assertSyncReasons(expectingTrades(sec(4), 5))

    resetEvents()

    observe(
      change(sec(2), o(-10, of = -10)),
      cancellation(sec(3), absoluteRest = Some(9)),
      change(sec(4), o(-5, of = -10)),
      disappearance(sec(5)),
    )
    assertSyncReasons(expectingTrades(sec(4), -5))
  }

  test("if trades implied by observation and cancel are the same, the earlier timestamp is used") {
    observe(
      change(sec(2), o(10, of = 10)),
      cancellation(sec(3), absoluteRest = Some(9)),
      change(sec(4), o(9, of = 10)),
      disappearance(sec(5)),
    )
    assertSyncReasons(expectingTrades(sec(3), 1))

    resetEvents()

    observe(
      change(sec(2), o(10, of = 10)),
      change(sec(3), o(9, of = 10)),
      cancellation(sec(4), absoluteRest = Some(9)),
      disappearance(sec(5)),
    )
    assertSyncReasons(expectingTrades(sec(3), 1))
  }

  test("if more trades are seen than the cancel implies an overfill error is generated, order is reported gone") {
    val eventsA = observe(
      change(sec(2), o(10, of = 10)),
      trade(sec(3), 1),
      trade(sec(4), 1),
      cancellation(sec(5), absoluteRest = Some(9)),
      disappearance(sec(6)),
    )
    assertOverfill(eventsA(2), totalFill = dec(2), maxFill = dec(1))
    assertNotReported()

    resetEvents()

    val eventsB = observe(
      change(sec(2), o(-10, of = -10)),
      trade(sec(3), -1),
      trade(sec(4), -1),
      cancellation(sec(5), absoluteRest = Some(9)),
      disappearance(sec(6)),
    )
    assertOverfill(eventsB(2), totalFill = dec(-2), maxFill = dec(-1))
    assertNotReported()
  }

  test("if cancel quantity is greater than full quantity from observation it yields inconsistent events") {
    val eventsA = observe(
      change(sec(1), o(10, of = 10)),
      cancellation(sec(2), absoluteRest = Some(11)),
    )
    assertNotReported()
    assertInconsistentEvents(eventsA(1), eventsA(0))

    resetEvents()

    val eventsB = observe(
      cancellation(sec(1), absoluteRest = Some(11)),
      change(sec(2), o(-10, of = -10)),
    )
    assertNotReported()
    assertInconsistentEvents(eventsB(0), eventsB(1))
  }

  test("if cancel quantity is greater than full quantity from creation it yields inconsistent events") {
    val eventsA = observe(
      disappearance(sec(0)),
      creation(sec(1), o(10, of = 10)),
      cancellation(sec(2), absoluteRest = Some(11)),
    )
    assertNotReported()
    assertInconsistentEvents(eventsA(2), eventsA(1))

    resetEvents()

    val eventsB = observe(
      disappearance(sec(0)),
      cancellation(sec(1), absoluteRest = Some(11)),
      creation(sec(2), o(-10, of = -10)),
    )
    assertNotReported()
    assertInconsistentEvents(eventsB(1), eventsB(2))
  }

  test("it yields inconsistent events when there are several cancels (last two)") {
    val events = observe(
      disappearance(sec(0)),
      cancellation(sec(2), absoluteRest = Some(10)),
      cancellation(sec(3), absoluteRest = Some(10)),
      cancellation(sec(4), absoluteRest = Some(10)),
    )
    assertNotReported()
    assertInconsistentEvents(events(2), events(3))
  }

  test("if the cancel quantity is unknown the state is syncing because trades might still be seen") {
    observe(
      disappearance(sec(0)),
      creation(sec(1), o(10, of = 10)),
      cancellation(sec(2), absoluteRest = None),
    )
    assertNotReported()
    assertSyncReasons(unknownIfMoreTradesBeforeCancel(sec(2)))

    resetEvents()

    observe(
      change(sec(1), o(-10, of = -10)),
      cancellation(sec(2), absoluteRest = None),
    )
    assertNotReported()
    assertSyncReasons(
      unknownIfMoreTradesBeforeCancel(sec(2)),
      expectingObservationChange(sec(2), None),
    )
  }

}
