package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.{order => bitfinexOrder}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.{ExchangeId, Market, Order, TradingPair}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BitfinexModelConverterTest_Order extends BasicTest {

  def convert(o: BitfinexOrder, eid: ExchangeId = ExchangeId("X")): Order =
    new BitfinexModelConverter(eid).convertOrder(o)

  test("the id is converted to a string") {
    convert(bitfinexOrder(id = 123)).id shouldEqual "123"
  }

  test("the amount is transferred as-is") {
    convert(bitfinexOrder(amount = dec("1.23"))).openQuantity shouldEqual dec("1.23")
    convert(bitfinexOrder(amount = dec("-1.23"))).openQuantity shouldEqual dec("-1.23")
  }

  test("the original amount is transferred as-is") {
    convert(bitfinexOrder(originalAmount = dec("2.34"))).fullQuantity shouldEqual dec("2.34")
    convert(bitfinexOrder(originalAmount = dec("-2.34"))).fullQuantity shouldEqual dec("-2.34")
  }

  test("the market is derived from the symbol and contains the exchange id") {
    convert(bitfinexOrder(symbol = "tIOTBTC"), ExchangeId("E123")).market shouldEqual
      Market(ExchangeId("E123"), TradingPair("IOT", "BTC"))
  }

  test("the price is simply transferred") {
    convert(bitfinexOrder(price = dec("0.07"))).price shouldBe dec("0.07")
  }

}
