package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.orderTracking.OrderTrackingEvent.ObservationChange
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.observationChange

class SingleOrderObservationHistoryTest extends BasicTest {

  private def history(changes: ObservationChange*) = SingleOrderObservationHistory(changes)

  test("an exception is thrown when created empty") {
    an[Exception] shouldBe thrownBy(history())
  }

  test("new observation changes can be appended") {
    val h0 = history(observationChange(sec(10)))
    val h1 = h0.append(observationChange(sec(11), order("A", 1)))
    val h2 = h1.append(observationChange(sec(12), order("A", 2)))
    h1 shouldEqual history(
      observationChange(sec(10)),
      observationChange(sec(11), order("A", 1)),
    )
    h2 shouldEqual history(
      observationChange(sec(10)),
      observationChange(sec(11), order("A", 1)),
      observationChange(sec(12), order("A", 2)),
    )
  }

  test("it remains the same when an appended change is redundant") {
    val h0 = history(observationChange(sec(10)))
    val h1 = h0.append(observationChange(sec(11)))
    val h2 = h1.append(observationChange(sec(12), order("A", 2)))
    val h3 = h2.append(observationChange(sec(13), order("A", 2)))
    val h4 = h3.append(observationChange(sec(14)))
    val h5 = h4.append(observationChange(sec(15)))
    h1 shouldBe theSameInstanceAs(h0)
    h3 shouldBe theSameInstanceAs(h2)
    h5 shouldBe theSameInstanceAs(h4)
  }

  test("it can tell if an order has disappeared") {
    val h0 = history(observationChange(sec(10)))
    val h1 = h0.append(observationChange(sec(12), order("A", 2)))
    val h2 = h1.append(observationChange(sec(12), order("A", 3)))
    val h3 = h2.append(observationChange(sec(14)))
    h0.hasDisappeared shouldEqual false
    h1.hasDisappeared shouldEqual false
    h2.hasDisappeared shouldEqual false
    h3.hasDisappeared shouldEqual true
  }

  test("it only counts as disappeared when it has finally disappeared, not temporarily (error on exchange side)") {
    val h0 = history(observationChange(sec(10)))
    val h1 = h0.append(observationChange(sec(12), order("A", 2)))
    val h2 = h1.append(observationChange(sec(14)))
    val h3 = h2.append(observationChange(sec(15), order("A", 3)))
    val h4 = h3.append(observationChange(sec(16)))
    h3.hasDisappeared shouldEqual false
    h4.hasDisappeared shouldEqual true
  }

  test("the last observation is available as an option") {
    val h0 = history(observationChange(sec(10)))
    val h1 = h0.append(observationChange(sec(12), order("A", 2)))
    val h2 = h1.append(observationChange(sec(14)))
    val h3 = h2.append(observationChange(sec(15), order("A", 3)))
    val h4 = h3.append(observationChange(sec(16)))
    h0.latestPresentObservation shouldEqual None
    h1.latestPresentObservation shouldEqual Some(order("A", 2))
    h2.latestPresentObservation shouldEqual Some(order("A", 2))
    h3.latestPresentObservation shouldEqual Some(order("A", 3))
    h4.latestPresentObservation shouldEqual Some(order("A", 3))
  }

}
