package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.orderTracking.helpers.BasicOrderTrackingStateTest

class BasicOrderTrackingStateTest_3_WithCreation extends BasicOrderTrackingStateTest {

  test("when only full creation is observed the order is not reported and expected to appear") {
    observe(creation(sec(1), o(10, of = 10)))
    observe(absence(sec(2)))
    assertNotReported()
    assertSyncReasons(expectingOrderToAppear(sec(1), o(10, of = 10)))
  }

  test("when only partial creation is observed the order is not reported, expected to appear and trades are expected") {
    observe(creation(sec(1), o(4, of = 10)))
    observe(absence(sec(2)))
    assertNotReported()
    assertSyncReasons(
      expectingOrderToAppear(sec(1), o(4, of = 10)),
      expectingTrades(sec(1), 6),
    )

    resetEvents()

    observe(creation(sec(1), o(-4, of = -10)))
    observe(absence(sec(2)))
    assertNotReported()
    assertSyncReasons(
      expectingOrderToAppear(sec(1), o(-4, of = -10)),
      expectingTrades(sec(1), -6),
    )
  }

  test("when only full creation and trades are observed the order remains not reported and syncing with correct size") {
    observe(
      creation(sec(1), o(10, of = 10)),
      absence(sec(2)),
      trade(sec(2), 2),
      trade(sec(3), 4),
    )
    assertNotReported()
    assertSyncReasons(expectingOrderToAppear(sec(3), o(4, of = 10)))
  }

  test("when partial creation and part of the implied trades are observed, some trades and appearance are expected") {
    observe(
      creation(sec(1), o(-4, of = -10)),
      absence(sec(2)),
      trade(sec(1), -2),
    )
    assertNotReported()
    assertSyncReasons(
      expectingOrderToAppear(sec(1), o(-4, of = -10)),
      expectingTrades(sec(1), -4),
    )
  }

  test("when partial creation and more than the implied trades are observed, the correct appearance is expected") {
    observe(
      creation(sec(1), o(-4, of = -10)),
      absence(sec(2)),
      trade(sec(1), -7),
    )
    assertNotReported()
    assertSyncReasons(
      expectingOrderToAppear(sec(1), o(-3, of = -10)),
    )
  }

  test("when only creation and trades that fill the order are seen it reported state is none and it is in sync") {
    observe(
      creation(sec(1), o(10, of = 10)),
      absence(sec(2)),
      trade(sec(2), 2),
      trade(sec(3), 8),
    )
    assertNotReported()
    assertInSync()
  }

  test("several different creations yield an inconsistent events error (with the last two creations)") {
    val events = observe(
      creation(sec(1), o(10, of = 10)),
      creation(sec(2), o(9, of = 9)),
      creation(sec(3), o(8, of = 8)),
      absence(sec(2)),
    )
    assertNotReported()
    assertInconsistentEvents(events(1), events(2))
  }

  test("several creations are also inconsistent when they are identical") {
    val events = observe(
      creation(sec(1), o(10, of = 10)),
      creation(sec(2), o(10, of = 10)),
      absence(sec(2)),
    )
    assertNotReported()
    assertInconsistentEvents(events(0), events(1))
  }

  test("it yields inconsistent events when the creation is not consistent with the last observation") {
    val events = observe(
      creation(sec(1), o(10, of = 10)),
      change(sec(2), o(9, of = 9)),
      change(sec(3), o(7, of = 9)),
    )
    assertReportedState(o(9, of = 9))
    assertInconsistentEvents(events(0), events(2))
  }

  test("an overfill error is yielded when total trade quantity exceeds the creation order size") {
    val eventsA = observe(
      absence(sec(0)),
      creation(sec(1), o(10, of = 10)),
      trade(sec(2), 5),
      trade(sec(3), 6),
    )
    assertNotReported()
    assertOverfill(eventsA(3), totalFill = 11, maxFill = 10)

    resetEvents()

    val eventsB = observe(
      absence(sec(0)),
      creation(sec(1), o(-10, of = -10)),
      trade(sec(2), -5),
      trade(sec(3), -6),
    )
    assertNotReported()
    assertOverfill(eventsB(3), totalFill = -11, maxFill = -10)
  }

}
