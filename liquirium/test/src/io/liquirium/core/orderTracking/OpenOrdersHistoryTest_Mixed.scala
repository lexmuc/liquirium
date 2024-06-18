package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{BasicTest, OrderHelpers}
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{observationChange, openOrdersHistory => history, openOrdersSnapshot => snapshot}


class OpenOrdersHistoryTest_Mixed extends BasicTest {

  def o(id: String, q: Int = 1): Order = OrderHelpers.order(id=id, quantity = dec(q))

  test("an exception is thrown for an empty history") {
    an[Exception] shouldBe thrownBy(history())
  }

  test("a history may be created from the initial snapshot") {
    OpenOrdersHistory.start(snapshot(sec(1), o("A"))) shouldEqual history(
      snapshot(sec(1), o("A")),
    )
  }

  test("snapshots must be properly ordered, otherwise an exception is thrown") {
    history(
      snapshot(sec(1), o("A")),
      snapshot(sec(2), o("B")),
    )
    an[Exception] shouldBe thrownBy(
      history(
        snapshot(sec(2), o("B")),
        snapshot(sec(1), o("A")),
      )
    )
  }

  test("consecutive snapshots may have the same timestamp") {
    history(
      snapshot(sec(1), o("A")),
      snapshot(sec(1), o("B")),
    )
  }

  test("snapshots can be appended one by one") {
    history(
      snapshot(sec(1), o("A"))
    ).appendIfChanged(
      snapshot(sec(2), o("B"))
    ) shouldEqual history(
      snapshot(sec(1), o("A")),
      snapshot(sec(2), o("B"))
    )
  }

  test("it gives access to the last snapshot") {
    history(
      snapshot(sec(1), o("A")),
      snapshot(sec(2), o("B")),
    ).lastSnapshot shouldEqual snapshot(sec(2), o("B"))
  }

  test("snapshots with redundant order information are ignored") {
    history(
      snapshot(sec(1), o("A")),
      snapshot(sec(2), o("A")),
      snapshot(sec(3), o("B")),
    ) shouldEqual history(
      snapshot(sec(1), o("A")),
      snapshot(sec(3), o("B")),
    )
  }

  test("the set of all observed order ids can be accessed") {
    history(
      snapshot(sec(1), o("A", 10)),
      snapshot(sec(2), o("A", 10), o("B")),
      snapshot(sec(3), o("A", 5)),
    ).observedIds shouldEqual Set("A", "B")
  }

  test("oder ids in the first snapshot are included in the set of ids") {
    history(
      snapshot(sec(1), o("A"), o("B")),
      snapshot(sec(2), o("A")),
    ).observedIds shouldEqual Set("A", "B")
  }

}
