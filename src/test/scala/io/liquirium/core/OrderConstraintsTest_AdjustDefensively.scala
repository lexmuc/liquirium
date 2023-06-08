package io.liquirium.core

import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.OperationIntentHelpers.orderIntent
import io.liquirium.util.NumberPrecision

class OrderConstraintsTest_AdjustDefensively extends BasicTest {

  private var pricePrecision: NumberPrecision = NumberPrecision.Infinite
  private var orderQuantityPrecision: NumberPrecision = NumberPrecision.Infinite

  def constraints: OrderConstraints = OrderConstraints(
    pricePrecision = pricePrecision,
    orderQuantityPrecision = orderQuantityPrecision,
  )

  def adjustDefensively(q: String, at: String): Option[OrderIntent] = {
    constraints.adjustDefensively(orderIntent(q, at = at))
  }

  test("a conforming order is left unchanged") {
    pricePrecision = NumberPrecision.MultipleOf(dec("1.0"))
    orderQuantityPrecision = NumberPrecision.multipleOf(dec("1.0"))
    adjustDefensively("2.0", at = "4.0") shouldEqual Some(orderIntent("2.0", at = "4.0"))
    adjustDefensively("-2.0", at = "4.0") shouldEqual Some(orderIntent("-2.0", at = "4.0"))
  }

  test("it can at most reduce the absolute quantity, not increase it") {
    orderQuantityPrecision = NumberPrecision.multipleOf(dec("1.0"))
    adjustDefensively("1.7", at = "2.0") shouldEqual Some(orderIntent("1.0", at = "2.0"))
    adjustDefensively("1.1", at = "2.0") shouldEqual Some(orderIntent("1.0", at = "2.0"))
    adjustDefensively("-1.1", at = "2.0") shouldEqual Some(orderIntent("-1.0", at = "2.0"))
    adjustDefensively("-1.7", at = "2.0") shouldEqual Some(orderIntent("-1.0", at = "2.0"))
  }

  test("if necessary the price is moved lower for buys and higher for sells") {
    pricePrecision = NumberPrecision.multipleOf(dec("0.1"))
    adjustDefensively("1", at = "2.11") shouldEqual Some(orderIntent("1", at = "2.1"))
    adjustDefensively("1", at = "2.19") shouldEqual Some(orderIntent("1", at = "2.1"))
    adjustDefensively("-1", at = "2.11") shouldEqual Some(orderIntent("-1", at = "2.2"))
    adjustDefensively("-1", at = "2.19") shouldEqual Some(orderIntent("-1", at = "2.2"))
  }

  test("price and quantity can be adjusted at once") {
    pricePrecision = NumberPrecision.multipleOf(dec("0.1"))
    orderQuantityPrecision = NumberPrecision.multipleOf(dec("1.0"))
    adjustDefensively("1.01", at = "2.11") shouldEqual Some(orderIntent("1", at = "2.1"))
  }

  test("when the quantity or the price would be zero it returns None") {
    pricePrecision = NumberPrecision.multipleOf(dec("0.1"))
    orderQuantityPrecision = NumberPrecision.multipleOf(dec("1.0"))
    adjustDefensively("0.1", at = "2.11") shouldEqual None
    adjustDefensively("-0.1", at = "2.11") shouldEqual None
    adjustDefensively("1.0", at = "0.01") shouldEqual None
  }

}
