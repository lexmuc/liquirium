package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{BasicTest, OrderHelpers}
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.openOrdersSnapshot

class OpenOrdersSnapshotTest extends BasicTest {

  def o(id: String, q: Int = 1): Order = OrderHelpers.order(id=id, quantity = dec(q))

  test("it allows to check whether an order is present") {
    val s = openOrdersSnapshot(sec(0), o("A", 3))
    s.contains("A") shouldBe true
    s.contains("B") shouldBe false
  }

  test("getting an order id from a snapshot yields an option containing the order if present") {
    val s = openOrdersSnapshot(sec(0), o("A", 3))
    s.get("A") shouldEqual Some(o("A", 3))
    s.get("B") shouldEqual None
  }

  test("the ids can be accessed directly") {
    val s = openOrdersSnapshot(sec(0), o("A", 3), o("B", 4))
    s.orderIds shouldEqual Set("A", "B")
  }

}
