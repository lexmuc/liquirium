package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.CoreHelpers.sec

class BasicOrderTrackingStateTest_0_Basics extends BasicOrderTrackingStateTest {

  test("an order is currently not observed if it has never appeared or it is observed to be gone") {
    observe(
      absence(sec(0)),
      creation(sec(1), o(10, of = 10))
    )
    assertIsCurrentlyNotObserved()

    observe(
      change(sec(2), o(10, of = 10)),
      absence(sec(3)),
    )
    assertIsCurrentlyNotObserved()
  }

  test("the order is currently observed if it is still observed even though it might have been cancelled") {
    observe(
      absence(sec(0)),
      creation(sec(1), o(10, of = 10)),
      change(sec(2), o(10, of = 10)),
    )
    assertIsCurrentlyObserved()

    observe(cancellation(sec(3)))
    assertIsCurrentlyObserved()
  }

}
