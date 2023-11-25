package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{typicalCurrenyPair, coinbaseOrder => co}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.MarketHelpers.eid
import io.liquirium.core.{Order, Side}

class CoinbaseModelConverterTest_Order extends BasicTest {

  val converter = new CoinbaseModelConverter(eid(234))

  private def convert(co: CoinbaseOrder) = converter.convertOrder(co)

  private def afterConversionOf(co: CoinbaseOrder)(a: Order => Unit): Unit = {
    a(convert(co.copy(side = Side.Buy)))
    a(convert(co.copy(side = Side.Sell)))
  }

  test("the quantity is set to the coinbase full quantity (negative for sell orders)") {
    val b = convert(co(side = Side.Buy, fullQuantity = dec("2.3"), filledQuantity = dec("1.3")))
    b.fullQuantity shouldEqual dec("2.3")
    val s = convert(co(side = Side.Sell, fullQuantity = dec("2.3"), filledQuantity = dec("1.3")))
    s.fullQuantity shouldEqual dec("-2.3")
  }

  test("the open quantity is set to the coinbase full quantity minus filled quantity (negative for sell orders)") {
    val b = convert(co(side = Side.Buy, fullQuantity = dec("2.3"), filledQuantity = dec("1.3")))
    b.openQuantity shouldEqual dec("1.0")
    val s = convert(co(side = Side.Sell, fullQuantity = dec("2.3"), filledQuantity = dec("1.3")))
    s.openQuantity shouldEqual dec("-1.0")
  }

  test("the order id is the coinbase order id") {
    afterConversionOf(co(orderId = "77577")) { _.id should equal("77577") }
  }

  test("the market is set on the order after being converted") {
    afterConversionOf(co(productId = typicalCurrenyPair)) { _.market shouldEqual
      converter.getMarket(typicalCurrenyPair) }
  }

  test("the price is set to the coinbase price") {
    afterConversionOf(co(price = dec("1.23"))) { _.price shouldEqual dec("1.23") }
  }

}
