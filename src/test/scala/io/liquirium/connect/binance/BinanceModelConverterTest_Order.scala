package io.liquirium.connect.binance

import io.liquirium.core.Side
import io.liquirium.connect.binance.helpers.BinanceTestHelpers.{order => bo}
import io.liquirium.core.helper.CoreHelpers.dec

class BinanceModelConverterTest_Order extends BinanceModelConverterTest {

  private def convert(o: BinanceOrder) = converter().convertOrder(o)

  test("it just assigns the id") {
    convert(bo(id = "ID123")).id shouldEqual "ID123"
  }

  test("the market is obtained via the converter itself") {
    convert(bo(symbol = "CADUSD")).market shouldEqual converter().getMarket("CADUSD")
  }

  test("the side determines the sign of the original quantity") {
    convert(bo(originalQuantity = dec(7), side = Side.Buy)).originalQuantity shouldEqual dec(7)
    convert(bo(originalQuantity = dec(8), side = Side.Sell)).originalQuantity shouldEqual dec(-8)
  }

  test("executed and original quantity determine the remaining quantity (side taken into account for sign)") {
    convert(bo(originalQuantity = dec(7), executedQuantity = dec(3), side = Side.Buy)).quantity shouldEqual dec(4)
    convert(bo(originalQuantity = dec(8), executedQuantity = dec(5), side = Side.Sell)).quantity shouldEqual dec(-3)
  }

  test("the price is just assigned") {
    convert(bo(price = dec(123))).price shouldEqual dec(123)
  }

}
