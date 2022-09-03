package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{BasicTest, OrderHelpers}
import io.liquirium.core.orderTracking.OrderTrackingEvent.ObservationChange
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{openOrdersSnapshot => snapshot, openOrdersHistory => history}

import java.time.Instant

class OpenOrdersSnapshotHistoryTest_SingleOrderObservationHistory extends BasicTest {

  def o(id: String, q: Int = 1): Order = OrderHelpers.order(id = id, quantity = dec(q))

  private def change(t: Instant, obs: Option[Order]) = ObservationChange(t, obs)

  test("an order that has not been seen has one change at the first timestamp (it's not there)") {
    val h = history(
      snapshot(sec(1), o("A")),
    )
    h.singleOrderHistory("B").changes shouldEqual Seq(
      change(sec(1), None),
    )
  }

  test("an order seen in the first and only snapshot has on observation") {
    val h = history(
      snapshot(sec(1), o("A")),
    )
    h.singleOrderHistory("A").changes shouldEqual Seq(
      change(sec(1), Some(o("A"))),
    )
  }

  test("changes occur every time an order changes, including disappearance and reappearance") {
    val h = history(
      snapshot(sec(1), o("A", 1)),
      snapshot(sec(2), o("A", 1)),
      snapshot(sec(3), o("A", 3)),
      snapshot(sec(4)),
      snapshot(sec(5)),
      snapshot(sec(6), o("A", 3)),
    )
    h.singleOrderHistory("A").changes shouldEqual Seq(
      change(sec(1), Some(o("A", 1))),
      change(sec(3), Some(o("A", 3))),
      change(sec(4), None),
      change(sec(6), Some(o("A", 3))),
    )
  }

  test("an order that appears at some point has the first (empty) observation at the first snapshot timestamp") {
    val h = history(
      snapshot(sec(1), o("A", 1)),
      snapshot(sec(2), o("A", 1)),
      snapshot(sec(3), o("A", 1), o("B", 1)),
    )
    h.singleOrderHistory("B").changes shouldEqual Seq(
      change(sec(1), None),
      change(sec(3), Some(o("B", 1))),
    )
  }

}
