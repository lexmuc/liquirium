package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.CoreHelpers.sec

class BasicOrderTrackingStateTest_3_WithCreation extends BasicOrderTrackingStateTest {

  test("when creation is observed the order remains syncing and not reported until the order is seen") {
    observe(creation(sec(1), o(10, of = 10)))
    observe(absence(sec(2)))
    assertNotReported()
    assertSyncReasons(unknownWhyOrderIsGone(sec(2)))

    observe(change(sec(3), o(10, of = 10)))
    assertReportedState(o(10, of = 10))
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
