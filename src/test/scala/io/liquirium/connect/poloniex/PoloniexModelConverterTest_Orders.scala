package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.{poloniexOrder => po, typicalCurrencyPair => typicalPair}
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.{Order, Side}

class PoloniexModelConverterTest_Orders extends PoloniexModelConverterTest {

  private def convert(po: PoloniexOrder) = converter().convertOrder(po)

  private def afterConversionOf(po: PoloniexOrder)(a: Order => Unit): Unit = {
    a(convert(po.copy(side = Side.Buy)))
    a(convert(po.copy(side = Side.Sell)))
  }

  test("the original quantity is set to the poloniex quantity (negative for sell orders)") {
    val b = convert(po(side = Side.Buy, quantity = dec("2.3"), filledQuantity = dec("1.3")))
    b.fullQuantity shouldEqual dec("2.3")
    val s = convert(po(side = Side.Sell, quantity = dec("2.3"), filledQuantity = dec("1.3")))
    s.fullQuantity shouldEqual dec("-2.3")
  }

  test("the open quantity is set to the poloniex quantity minus filled quantity (negative for sell orders)") {
    val b = convert(po(side = Side.Buy, quantity = dec("2.3"), filledQuantity = dec("1.3")))
    b.openQuantity shouldEqual dec("1.0")
    val s = convert(po(side = Side.Sell, quantity = dec("2.3"), filledQuantity = dec("1.3")))
    s.openQuantity shouldEqual dec("-1.0")
  }

  test("the id is the poloniex id") {
    afterConversionOf(po(id = "77577")) { _.id should equal("77577") }
  }

  test("the market is set on the order after being converted") {
    afterConversionOf(po(symbol = typicalPair)) { _.market shouldEqual converter().getMarket(typicalPair) }
  }

  test("the price is set to the poloniex price") {
    afterConversionOf(po(price = dec("1.23"))) { _.price shouldEqual dec("1.23") }
  }
}
