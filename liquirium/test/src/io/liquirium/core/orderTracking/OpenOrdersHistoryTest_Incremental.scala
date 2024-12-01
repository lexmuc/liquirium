package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{BasicTest, OrderHelpers}
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{openOrdersSnapshot => snapshot}
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, theSameInstanceAs}

class OpenOrdersHistoryTest_Incremental extends BasicTest {

  def o(id: String, q: Int = 1): Order = OrderHelpers.order(id=id, quantity = dec(q))

  test("the previous history is None for the root and the previous history (if last update changed something)") {
    val h0 = OpenOrdersHistory.start(snapshot(sec(1), o("A")))
    val h1 = h0.appendIfChanged(snapshot(sec(2), o("A")))
    val h2 = h1.appendIfChanged(snapshot(sec(3), o("B")))
    val h3 = h2.appendIfChanged(snapshot(sec(4), o("B")))
    val h4 = h3.appendIfChanged(snapshot(sec(5), o("C")))
    h0.prev shouldBe None
    h1.prev shouldBe None
    h2.prev.get shouldBe theSameInstanceAs(h0)
    h3.prev.get shouldBe theSameInstanceAs(h0)
    h4.prev.get shouldBe theSameInstanceAs(h2)
  }

  test("the root is always the first history") {
    val h0 = OpenOrdersHistory.start(snapshot(sec(1), o("A")))
    val h1 = h0.appendIfChanged(snapshot(sec(2), o("A")))
    val h2 = h0.appendIfChanged(snapshot(sec(3), o("B")))
    h0.root shouldBe theSameInstanceAs(h0)
    h1.root shouldBe theSameInstanceAs(h0)
    h2.root shouldBe theSameInstanceAs(h0)
  }

  test("appending a snapshot is the same as incrementing") {
    val h0 = OpenOrdersHistory.start(snapshot(sec(1), o("A")))
    val h1 = h0.appendIfChanged(snapshot(sec(2), o("A")))
    val h2 = h1.appendIfChanged(snapshot(sec(3), o("B")))
    h0.inc(snapshot(sec(2), o("A"))) shouldEqual h1
    h1.inc(snapshot(sec(3), o("B"))) shouldEqual h2
  }

  test("the last increment is the last recorded snapshot except for the root which has no last increment") {
    val h0 = OpenOrdersHistory.start(snapshot(sec(1), o("A")))
    val h1 = h0.appendIfChanged(snapshot(sec(2), o("A")))
    val h2 = h1.appendIfChanged(snapshot(sec(3), o("B")))
    val h3 = h2.appendIfChanged(snapshot(sec(4), o("B")))
    h0.lastIncrement shouldBe None
    h1.lastIncrement shouldBe None
    h2.lastIncrement.get shouldEqual snapshot(sec(3), o("B"))
    h3.lastIncrement.get shouldEqual snapshot(sec(3), o("B"))
  }

}
