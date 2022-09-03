package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.CoreHelpers.sec

class BasicOrderTrackingStateTest_2_ObservationsAndTrades extends BasicOrderTrackingStateTest {

  test("when only trades are observed the reporting state is absent and it is syncing since the last trade") {
    observe(absence(sec(1)))
    observe(trade(sec(2), 1))
    assertNotReported()
    assertSyncReasons(unknownWhyOrderIsGone(sec(2)))

    observe(trade(sec(3), 2))
    assertNotReported()
    assertSyncReasons(unknownWhyOrderIsGone(sec(3)))
  }

  test("when order and trades are in sync, it outputs the reduced order and is in sync, too") {
    observe(change(sec(1), o(2, of = 10)))
    observe(trade(sec(2), 3))
    observe(trade(sec(2), 5))
    assertReportedState(o(2, of = 10))
    assertInSync()

    resetEvents()

    observe(change(sec(1), o(-2, of = -10)))
    observe(trade(sec(2), -3))
    observe(trade(sec(2), -5))
    assertReportedState(o(-2, of = -10))
    assertInSync()
  }

  test("the reported quantity only changes with the trades, not observations (for buy and sell orders)") {
    observe(change(sec(1), o(5, of = 10)))
    assertReportedState(o(10, of = 10))
    observe(trade(sec(2), 5))
    assertReportedState(o(5, of = 10))
    observe(trade(sec(3), 3))
    assertReportedState(o(2, of = 10))
    observe(change(sec(4), o(1, of = 10)))
    assertReportedState(o(2, of = 10))

    resetEvents()
    observe(change(sec(1), o(-5, of = -10)))
    assertReportedState(o(-10, of = -10))
    observe(trade(sec(2), -5))
    assertReportedState(o(-5, of = -10))
    observe(trade(sec(3), -3))
    assertReportedState(o(-2, of = -10))
    observe(change(sec(4), o(-1, of = -10)))
    assertReportedState(o(-2, of = -10))
  }

  test("sync state is in sync, expecting trades, or waiting for order change depending on states and observation") {
    observe(change(sec(1), o(5, of = 10)))
    assertSyncReasons(expectingTrades(sec(1), 5))
    observe(trade(sec(2), 5))
    assertInSync()
    observe(trade(sec(3), 3))
    assertSyncReasons(expectingObservationChange(sec(3), Some(o(2, of = 10))))
    observe(change(sec(4), o(1, of = 10)))
    assertSyncReasons(expectingTrades(sec(4), 1))

    resetEvents()

    observe(change(sec(1), o(-5, of = -10)))
    assertSyncReasons(expectingTrades(sec(1), -5))
    observe(trade(sec(2), -5))
    assertInSync()
    observe(trade(sec(3), -3))
    assertSyncReasons(expectingObservationChange(sec(3), Some(o(-2, of = -10))))
    observe(change(sec(4), o(-1, of = -10)))
    assertSyncReasons(expectingTrades(sec(4), -1))
  }

  test("when an order is fully filled with trades it is expected to disappear") {
    observe(change(sec(1), o(6, of = 10)))
    observe(trade(sec(2), 4))
    observe(trade(sec(3), 6))
    assertSyncReasons(expectingObservationChange(sec(3), None))

    resetEvents()

    observe(change(sec(1), o(-6, of = -10)))
    observe(trade(sec(2), -4))
    observe(trade(sec(3), -6))
    assertSyncReasons(expectingObservationChange(sec(3), None))
  }

  test("when an order is fully filled and actually disappeared, the sync state is in sync and no order is reported") {
    observe(change(sec(1), o(10, of = 10)))
    observe(trade(sec(2), 10))
    observe(absence(sec(3)))
    assertInSync()
    assertNotReported()

    resetEvents()

    observe(change(sec(1), o(-10, of = -10)))
    observe(trade(sec(2), -10))
    observe(absence(sec(3)))
    assertInSync()
    assertNotReported()
  }

  test("when an order disappears without being fully filled, it is reported gone but syncing (unknown why gone)") {
    observe(change(sec(1), o(10, of = 10)))
    observe(trade(sec(2), 8))
    observe(absence(sec(3)))
    assertNotReported()
    assertSyncReasons(unknownWhyOrderIsGone(sec(3)))

    resetEvents()

    observe(change(sec(1), o(-10, of = -10)))
    observe(trade(sec(2), -8))
    observe(absence(sec(3)))
    assertNotReported()
    assertSyncReasons(unknownWhyOrderIsGone(sec(3)))
  }

  test("an overfill error is yielded when total trade quantity exceeds the observed order capacity") {
    val eventsA = observe(
      change(sec(1), o(10, of = 10)),
      trade(sec(2), 5),
      trade(sec(3), 6),
    )
    assertNotReported()
    assertOverfill(eventsA(2), totalFill = 11, maxFill = 10)

    resetEvents()

    val eventsB = observe(
      change(sec(1), o(-10, of = -10)),
      trade(sec(2), -5),
      trade(sec(3), -6),
    )
    assertNotReported()
    assertOverfill(eventsB(2), totalFill = -11, maxFill = -10)
  }

}
