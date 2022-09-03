package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.CoreHelpers.sec

class BasicOrderTrackingStateTest_1_OnlyObservations extends BasicOrderTrackingStateTest {

  test("it is reportable as-is without inconsistencies when it is only observed once in full") {
    observe(change(sec(2), o(1, of = 1)))
    assertReportedState(o(1, of = 1))
    assertInSync()
  }

  test("when it is observed with reduced quantity (buy/sell) it is reported full and sync state is expecting trades") {
    observe(change(sec(2), o(2, of = 10)))
    assertReportedState(o(10, of = 10))
    assertSyncReasons(expectingTrades(sec(2), 8))

    resetEvents()

    observe(change(sec(2), o(-2, of = -10)))
    assertReportedState(o(-10, of = -10))
    assertSyncReasons(expectingTrades(sec(2), -8))
  }

  test("when the amount is reduced several times, the last observation determines the expected trades") {
    observe(change(sec(2), o(5, of = 10)))
    observe(change(sec(3), o(3, of = 10)))
    observe(change(sec(4), o(2, of = 10)))
    assertReportedState(o(10, of = 10))
    assertSyncReasons(expectingTrades(sec(4), 8))

    resetEvents()

    observe(change(sec(2), o(-5, of = -10)))
    observe(change(sec(3), o(-3, of = -10)))
    observe(change(sec(4), o(-2, of = -10)))
    assertReportedState(o(-10, of = -10))
    assertSyncReasons(expectingTrades(sec(4), -8))
  }

  test("when the amount increases the full amount is reported but there is an inconsistent events error") {
    val obsB1 = change(sec(2), o(5, of = 10))
    val obsB2 = change(sec(3), o(6, of = 10))
    observe(obsB1)
    observe(obsB2)
    assertReportedState(o(10, of = 10))
    assertInconsistentEvents(obsB1, obsB2)

    resetEvents()

    val obsS1 = change(sec(2), o(-5, of = -10))
    val obsS2 = change(sec(3), o(-6, of = -10))
    observe(obsS1)
    observe(obsS2)
    assertReportedState(o(-10, of = -10))
    assertInconsistentEvents(obsS1, obsS2)
  }

  test("an inconsistency is found when the total order size has changed") {
    val obsB1 = change(sec(2), o(5, of = 10))
    val obsB2 = change(sec(3), o(4, of = 8))
    observe(obsB1)
    observe(obsB2)
    assertReportedState(o(8, of = 8))
    assertInconsistentEvents(obsB1, obsB2)
  }

  test("inconsistent observations are also found when they are further in the past") {
    val obs1 = change(sec(1), o(5, of = 10))
    val obs2 = change(sec(2), o(6, of = 10))
    val obs3 = change(sec(3), o(4, of = 10))
    observe(obs1, obs2, obs3)
    assertReportedState(o(10, of = 10))
    assertInconsistentEvents(obs1, obs2)
  }

  test("a reappearing order inconsistency is found when an order has disappeared and is observed again") {
    val events = observe(
     change(sec(1), o(5, of = 10)),
     absence(sec(2)),
     change(sec(3), o(4, of = 10)),
    )
    assertReportedState(o(10, of = 10))
    assertReappearingOrderInconsistency(events(2))
  }

  test("when an order disappears without being filled it is reported as gone, sync state is unknown why gone") {
    observe(
      change(sec(1), o(10, of = 10)),
      absence(sec(2)),
    )
    assertNotReported()
    assertSyncReasons(unknownWhyOrderIsGone(sec(2)))
  }

  test("when trades are expected for a disappeared order, both sync reasons are given") {
    observe(change(sec(1), o(10, of = 10)))
    observe(change(sec(2), o(8, of = 10)))
    observe(absence(sec(3)))
    assertNotReported()
    assertSyncReasons(
      unknownWhyOrderIsGone(sec(3)),
      expectingTrades(sec(2), 2)
    )

    resetEvents()

    observe(change(sec(1), o(-10, of = -10)))
    observe(change(sec(2), o(-8, of = -10)))
    observe(absence(sec(3)))
    assertNotReported()
    assertSyncReasons(
      unknownWhyOrderIsGone(sec(3)),
      expectingTrades(sec(2), -2)
    )
  }

}
