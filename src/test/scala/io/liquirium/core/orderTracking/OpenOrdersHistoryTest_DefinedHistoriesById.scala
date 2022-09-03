package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{BasicTest, OrderHelpers}
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers._

class OpenOrdersHistoryTest_DefinedHistoriesById extends BasicTest {

  def o(id: String, q: Int = 1): Order = OrderHelpers.order(id = id, quantity = dec(q))

  test("the defined histories are empty for an empty open orders history") {
    openOrdersHistory(
      openOrdersSnapshot(sec(1)),
    ).definedHistoriesById.mapValue shouldEqual Map()
  }

  test("the observations of the first snapshot are included in the individual histories") {
    openOrdersHistory(
      openOrdersSnapshot(sec(1), o("A", 1), o("B", 2)),
    ).definedHistoriesById.mapValue shouldEqual Map(
      "A" -> singleOrderObservationHistory(
        observationChange(sec(1), o("A", 1))
      ),
      "B" -> singleOrderObservationHistory(
        observationChange(sec(1), o("B", 2))
      ),
    )
  }

  test("further observations changes are reflected in the individual histories (change and disappearance)") {
    openOrdersHistory(
      openOrdersSnapshot(sec(1), o("A", 1), o("B", 2), o("C", 3)),
      openOrdersSnapshot(sec(2), o("B", 2), o("C", 4)),
    ).definedHistoriesById.mapValue shouldEqual Map(
      "A" -> singleOrderObservationHistory(
        observationChange(sec(1), o("A", 1)),
        observationChange(sec(2)),
      ),
      "B" -> singleOrderObservationHistory(
        observationChange(sec(1), o("B", 2)),
      ),
      "C" -> singleOrderObservationHistory(
        observationChange(sec(1), o("C", 3)),
        observationChange(sec(2), o("C", 4)),
      ),
    )
  }

  test("orders appearing later start with an empty observation with the first timestamp") {
    openOrdersHistory(
      openOrdersSnapshot(sec(1)),
      openOrdersSnapshot(sec(2), o("B", 2)),
    ).definedHistoriesById.mapValue("B") shouldEqual singleOrderObservationHistory(
      observationChange(sec(1)),
      observationChange(sec(2), o("B", 2)),
    )
  }

  test("individual histories not affected by changes remain the same (instances)") {
    val ooh0 = openOrdersHistory(
      openOrdersSnapshot(sec(1)),
      openOrdersSnapshot(sec(2), o("B", 2)),
    )
    val historyB  = ooh0.definedHistoriesById.mapValue("B")
    val ooh1 = ooh0.inc(openOrdersSnapshot(sec(3), o("B", 2), o("C", 3)))
    ooh1.definedHistoriesById.mapValue("B") shouldBe theSameInstanceAs(historyB)
  }

}
