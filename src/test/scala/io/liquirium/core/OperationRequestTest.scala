package io.liquirium.core

import io.liquirium.bot.helpers.OperationRequestHelpers.orderRequest
import io.liquirium.core
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.MarketHelpers.{market => m}
import io.liquirium.core.helpers.OrderHelpers.{modifiers => mod}

class OperationRequestTest extends BasicTest {

  test("request quantity can be changed") {
    OrderRequest(m("x"), dec(12), price = 7.2, mod(1)).changeQuantity(dec(23)) shouldEqual
      OrderRequest(m("x"), dec(23), price = 7.2, mod(1))
    OrderRequest(m("y"), dec(-12), price = 9.2, mod(2)).changeQuantity(dec(-23)) shouldEqual
      OrderRequest(m("y"), dec(-23), price = 9.2, mod(2))
  }

  test("buy and sell requests can be converted to exact orders with the given order id") {
    OrderRequest(m("x"), quantity = 4.1, price = 7.1, mod(1)).toExactOrder("abc") shouldEqual
      Order("abc", m("x"), openQuantity = 4.1, fullQuantity = 4.1, price = 7.1)
    OrderRequest(m("y"), quantity = -4.2, price = 9.3, mod(2)).toExactOrder("xyz") shouldEqual
      core.Order("xyz", m("y"), openQuantity = -4.2, fullQuantity = -4.2, price = 9.3)
  }

  test("modifiers can be set") {
    OrderRequest(m("x"), 4, price = 7, mod(1)).setModifiers(mod(2)) shouldEqual
      OrderRequest(m("x"), 4, price = 7, mod(2))
    OrderRequest(m("x"), -4, price = 7, mod(1)).setModifiers(mod(2)) shouldEqual
      OrderRequest(m("x"), -4, price = 7, mod(2))
  }

  test("isBuy- and isSell-flags depend on the quantity") {
    orderRequest(quantity = dec(2)).isBuy shouldBe true
    orderRequest(quantity = dec(2)).isSell shouldBe false
    orderRequest(quantity = dec(-2)).isBuy shouldBe false
    orderRequest(quantity = dec(-2)).isSell shouldBe true
  }

  test("an exception is thrown when it is attempted to create a request with quantity zero") {
    an[Exception] shouldBe thrownBy(orderRequest(quantity = BigDecimal(0)))
  }

}
