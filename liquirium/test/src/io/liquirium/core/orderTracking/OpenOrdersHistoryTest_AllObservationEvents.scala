package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{BasicTest, OrderHelpers}
import io.liquirium.core.orderTracking.OrderTrackingEvent.OrderObservationEvent
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{openOrdersSnapshot => snapshot}
import io.liquirium.eval.IncrementalSeq
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{observationChange => change, disappearance}

class OpenOrdersHistoryTest_AllObservationEvents extends BasicTest {

  def o(id: String, q: Int = 1): Order = OrderHelpers.order(id = id, quantity = dec(q))

  test("the initial sequence of all events is empty if there are no orders") {
    val h0 = OpenOrdersHistory.start(snapshot(sec(1)))
    h0.allObservationEvents shouldEqual IncrementalSeq.empty[OrderObservationEvent]
  }

  test("the initial sequence of all events contains observation changes for all initial orders alphabetically") {
    val h0 = OpenOrdersHistory.start(snapshot(sec(1), o("C"), o("A"), o("B")))
    h0.allObservationEvents shouldEqual IncrementalSeq(
      change(sec(1), o("A")),
      change(sec(1), o("B")),
      change(sec(1), o("C")),
    )
  }

  test("when a snapshot is added changes in the orders are added (ordered by order-id)") {
    val snap1 = snapshot(sec(1), o("A", 1), o("B", 1), o("C", 1))
    val snap2 = snapshot(sec(2), o("A", 2), o("B", 2), o("C", 2))
    val h0 = OpenOrdersHistory.start(snap1)
    val h1 = h0.appendIfChanged(snap2)
    h1.allObservationEvents shouldEqual IncrementalSeq(
      change(sec(1), o("A", 1)),
      change(sec(1), o("B", 1)),
      change(sec(1), o("C", 1)),
      change(sec(2), o("A", 2)),
      change(sec(2), o("B", 2)),
      change(sec(2), o("C", 2)),
    )
  }

  test("no events are added for unchanged orders") {
    val snap1 = snapshot(sec(1), o("A", 1), o("B", 1), o("C", 1))
    val snap2 = snapshot(sec(2), o("A", 1), o("B", 1), o("C", 2))
    val h0 = OpenOrdersHistory.start(snap1)
    val h1 = h0.appendIfChanged(snap2)
    h1.allObservationEvents shouldEqual IncrementalSeq(
      change(sec(1), o("A", 1)),
      change(sec(1), o("B", 1)),
      change(sec(1), o("C", 1)),
      change(sec(2), o("C", 2)),
    )
  }

  test("new orders lead to new events ordered with the changes") {
    val snap1 = snapshot(sec(1), o("B", 1), o("C", 1))
    val snap2 = snapshot(sec(2), o("A", 1), o("B", 2), o("C", 2))
    val h0 = OpenOrdersHistory.start(snap1)
    val h1 = h0.appendIfChanged(snap2)
    h1.allObservationEvents shouldEqual IncrementalSeq(
      change(sec(1), o("B", 1)),
      change(sec(1), o("C", 1)),
      change(sec(2), o("A", 1)),
      change(sec(2), o("B", 2)),
      change(sec(2), o("C", 2)),
    )
  }

  test("disappearance events are added for orders that disappear") {
    val snap1 = snapshot(sec(1), o("A", 1), o("B", 1), o("C", 1))
    val snap2 = snapshot(sec(2), o("B", 2), o("C", 1))
    val h0 = OpenOrdersHistory.start(snap1)
    val h1 = h0.appendIfChanged(snap2)
    h1.allObservationEvents shouldEqual IncrementalSeq(
      change(sec(1), o("A", 1)),
      change(sec(1), o("B", 1)),
      change(sec(1), o("C", 1)),
      disappearance(sec(2), "A"),
      change(sec(2), o("B", 2)),
    )
  }

  test("the returned sequence is in fact incremental") {
    val snap1 = snapshot(sec(1), o("A", 1))
    val snap2 = snapshot(sec(2), o("B", 2))
    val h0 = OpenOrdersHistory.start(snap1)
    val h1 = h0.appendIfChanged(snap2)
    h1.allObservationEvents.latestCommonAncestor(h0.allObservationEvents).get shouldBe
      theSameInstanceAs(h0.allObservationEvents)
  }

}
