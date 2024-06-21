package io.liquirium.core.orderTracking

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.orderTracking.OrderTrackingEvent.OrderObservationEvent
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{disappearance, observationChange}

class SingleOrderObservationHistoryTest extends BasicTest {

  private def history(changes: OrderObservationEvent*) = SingleOrderObservationHistory(changes)

  test("it may be created empty") {
    history()
  }

  test("new observation changes can be appended") {
    val h0 = history()
    val h1 = h0.append(observationChange(sec(11), order("A", 1)))
    val h2 = h1.append(observationChange(sec(12), order("A", 2)))
    h1 shouldEqual history(
      observationChange(sec(11), order("A", 1)),
    )
    h2 shouldEqual history(
      observationChange(sec(11), order("A", 1)),
      observationChange(sec(12), order("A", 2)),
    )
  }

  test("it remains the same when an appended change is redundant") {
    val h0 = history()
    val h1 = h0.append(observationChange(sec(12), order("A", 2)))
    val h2 = h1.append(observationChange(sec(13), order("A", 2)))
    val h3 = h2.append(disappearance(sec(14), "A"))
    val h4 = h3.append(disappearance(sec(15), "A"))
    h2 shouldBe theSameInstanceAs(h1)
    h4 shouldBe theSameInstanceAs(h3)
  }

}
