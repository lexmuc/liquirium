package io.liquirium.core

import io.liquirium.core.helpers.OrderHelpers.{basicOrderData, order}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec

class OrderTest extends BasicTest {

 test("the quantity can be reset") {
    order(quantity = dec("-1.23"), originalQuantity = dec("-2.77")).resetQuantity shouldEqual
      order(quantity = dec("-2.77"), originalQuantity = dec("-2.77"))
  }

  test("the quantity can be changed") {
    order(quantity = dec("-1.23"), originalQuantity = dec("-2.77")).setQuantity(dec("-0.99")) shouldEqual
      order(quantity = dec("-0.99"), originalQuantity = dec("-2.77"))
  }

  test("the quantity can be reduced") {
    order(quantity = dec("-1.23"), originalQuantity = dec("-2.77"))
      .reduceQuantity(dec("-0.99")) shouldEqual
      order(quantity = dec("-0.24"), originalQuantity = dec("-2.77"))
    order(quantity = dec("1.23"), originalQuantity = dec("2.77"))
      .reduceQuantity(dec("0.99")) shouldEqual
      order(quantity = dec("0.24"), originalQuantity = dec("2.77"))
  }

  test("the quantity cannot be reduced to zero") {
    order(quantity = dec("-1.23"), originalQuantity = dec("-2.77"))
      .reduceQuantity(dec("-1.23")) shouldEqual
      order(quantity = dec("0"), originalQuantity = dec("-2.77"))
    order(quantity = dec("1.23"), originalQuantity = dec("2.77"))
      .reduceQuantity(dec("1.23")) shouldEqual
      order(quantity = dec("0"), originalQuantity = dec("2.77"))
  }

  test("an order with positive quantity is a buy order, not a sell order") {
    order(quantity = dec("2.0")).isBuy shouldEqual true
    order(quantity = dec("2.0")).isSell shouldEqual false
  }

  test("an order with negative quantity is a sell order, not a buy order") {
    order(quantity = dec("-2.0")).isSell shouldEqual true
    order(quantity = dec("-2.0")).isBuy shouldEqual false
  }

  test("the volume is the absolute quantity multiplied with the price") {
    order(quantity = dec("2.5"), price = dec("1.5")).volume shouldEqual dec("3.75")
    order(quantity = dec("-2.5"), price = dec("1.5")).volume shouldEqual dec("3.75")
  }

  test("sell and buy flags are set properly for instances of BasicOrderData") {
    basicOrderData("X", dec("1.0") -> dec("2.0")).isBuy shouldBe true
    basicOrderData("X", dec("1.0") -> dec("2.0")).isSell shouldBe false
    basicOrderData("X", dec("1.0") -> dec("-2.0")).isBuy shouldBe false
    basicOrderData("X", dec("1.0") -> dec("-2.0")).isSell shouldBe true
  }

  test("the filled quantity can be determined for buy and sell orders") {
    order(quantity = dec("2.5"), originalQuantity = dec("3.0")).filledQuantity shouldEqual dec("0.5")
    order(quantity = dec("-2.5"), originalQuantity = dec("-3.0")).filledQuantity shouldEqual dec("-0.5")
  }

}
