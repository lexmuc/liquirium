package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{BasicTest, OrderHelpers}
import io.liquirium.core.orderTracking.OrderTrackingEvent.{Disappearance, ObservationChange, OrderObservationEvent}
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{openOrdersHistory => history, openOrdersSnapshot => snapshot}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant

class OpenOrdersSnapshotHistoryTest_SingleOrderObservationHistory extends BasicTest {

  def o(id: String, q: Int = 1): Order = OrderHelpers.order(id = id, quantity = dec(q))

  private def change(t: Instant, o: Order) = ObservationChange(t, o)
  private def disappearance(t: Instant, id: String) = Disappearance(t, id)

  test("an order that has not been seen has no events") {
    val h = history(
      snapshot(sec(1), o("A")),
    )
    h.singleOrderHistory("B").changes shouldEqual Seq(
    )
  }

  test("an order seen in the first and only snapshot has on observation") {
    val h = history(
      snapshot(sec(1), o("A")),
    )
    h.singleOrderHistory("A").changes shouldEqual Seq(
      change(sec(1), o("A")),
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
      change(sec(1), o("A", 1)),
      change(sec(3), o("A", 3)),
      disappearance(sec(4), "A"),
      change(sec(6), o("A", 3)),
    )
  }

}
