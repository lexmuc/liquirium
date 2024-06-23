package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.orderTracking.helpers.BasicOrderTrackingStateTest

class BasicOrderTrackingStateTest_0_Basics extends BasicOrderTrackingStateTest {

  test("the creation events are accessible") {
    basicState.creationEvents shouldEqual Seq()
    observe(
      creation(sec(1), o(10, of = 10)),
      creation(sec(2), o(-10, of = -10)),
    )
    basicState.creationEvents shouldEqual Seq(
      creation(sec(1), o(10, of = 10)),
      creation(sec(2), o(-10, of = -10)),
    )
  }

  test("the latest observation event is accessible as an option") {
    basicState.creation shouldEqual None
    observe(creation(sec(1), o(10, of = 10)))
    basicState.creation shouldEqual Some(creation(sec(1), o(10, of = 10)))
    observe(creation(sec(2), o(-4, of = -8)))
    basicState.creation shouldEqual Some(creation(sec(2), o(-4, of = -8)))
  }

  test("the latest creation event is returned regardless of event order") {
    observe(
      creation(sec(3), o(10, of = 10)),
      creation(sec(2), o(8, of = 8)),
    )
    basicState.creation shouldEqual Some(creation(sec(3), o(10, of = 10)))
  }

  test("in case two creation events have the same timestamp, the later one is picked") {
    observe(
      creation(sec(2), o(10, of = 10)),
      creation(sec(2), o(8, of = 8)),
    )
    basicState.creation shouldEqual Some(creation(sec(2), o(8, of = 8)))
  }

  test("an order is currently not observed if it has never appeared or it is observed to be gone") {
    observe(
      creation(sec(1), o(10, of = 10))
    )
    assertIsCurrentlyNotObserved()

    observe(
      change(sec(2), o(10, of = 10)),
      disappearance(sec(3)),
    )
    assertIsCurrentlyNotObserved()
  }

  test("the order is currently observed if it is still observed even though it might have been cancelled") {
    observe(
      creation(sec(1), o(10, of = 10)),
      change(sec(2), o(10, of = 10)),
    )
    assertIsCurrentlyObserved()

    observe(cancellation(sec(3)))
    assertIsCurrentlyObserved()
  }

  test("the latest present order observation change is available as an option") {
    basicState.latestPresentObservationChange shouldBe None
    observe(change(sec(1), o(10, of = 10)))
    basicState.latestPresentObservationChange shouldEqual Some(change(sec(1), o(10, of = 10)))
    observe(change(sec(2), o(8, of = 10)))
    basicState.latestPresentObservationChange shouldEqual Some(change(sec(2), o(8, of = 10)))
    observe(disappearance(sec(3)))
    basicState.latestPresentObservationChange shouldEqual Some(change(sec(2), o(8, of = 10)))
  }

  test("the latest present order state is available as an option") {
    basicState.latestPresentState shouldBe None
    observe(disappearance(sec(0)))
    basicState.latestPresentState shouldBe None
    observe(change(sec(1), o(10, of = 10)))
    basicState.latestPresentState shouldEqual Some(o(10, of = 10))
    observe(change(sec(2), o(8, of = 10)))
    basicState.latestPresentState shouldEqual Some(o(8, of = 10))
    observe(disappearance(sec(3)))
    basicState.latestPresentState shouldEqual Some(o(8, of = 10))
  }

  test("it exposes a flag that indicates whether the order is currently observed") {
    basicState.isCurrentlyObserved shouldBe false
    observe(disappearance(sec(0)))
    basicState.isCurrentlyObserved shouldBe false
    observe(change(sec(1), o(10, of = 10)))
    basicState.isCurrentlyObserved shouldBe true
    observe(change(sec(2), o(8, of = 10)))
    basicState.isCurrentlyObserved shouldBe  true
    observe(disappearance(sec(3)))
    basicState.isCurrentlyObserved shouldBe false
  }

}
