package io.liquirium.core

import io.liquirium.core.helper.{BasicTest, TradeHelpers}
import io.liquirium.core.helper.CoreHelpers.dec
import io.liquirium.core.helper.OrderHelpers.order

class OrderSetTest_RecordingTrades extends BasicTest {

  def orders(oo: Order*): OrderSet = OrderSet(oo.toSet)

  def trade(oid: Option[String] = None, amount: BigDecimal): Trade =
    TradeHelpers.trade(orderId = oid, quantity = amount)

  test("a sold quantity is deducted from the respective sell order") {
    orders(order(id = "s1", originalQuantity = dec("-4"), quantity = dec("-2"), price = dec("20")), order(234))
      .record(trade(Some("s1"), dec("-1.5"))) shouldEqual orders(
      order(id = "s1", quantity = dec("-0.5"), originalQuantity = dec("-4"), price = dec("20")),
      order(234)
    )
  }

  test("sell orders are removed when filled completely") {
    orders(order(id = "s1", originalQuantity = dec("-4"), quantity = dec("-2"), price = dec("20")), order(234))
      .record(trade(Some("s1"), dec("-2"))) shouldEqual orders(order(234))
  }

  test("a bought quantity is deducted from the respective buy order") {
    orders(order(id = "b1", originalQuantity = dec("4"), quantity = dec("2"), price = dec("20")), order(234))
      .record(trade(Some("b1"), dec("1.5"))) shouldEqual orders(
      order(id = "b1", quantity = dec("0.5"), originalQuantity = dec("4"), price = dec("20")),
      order(234)
    )
  }

  test("buy orders are removed when filled completely") {
    orders(order(id = "b1", originalQuantity = dec("4"), quantity = dec("2"), price = dec("20")), order(234))
      .record(trade(Some("b1"), dec("2"))) shouldEqual orders(order(234))
  }

  test("recording trades without order id has no effect") {
    orders(order(123), order(234)).record(trade(None, dec(2))) shouldEqual
      orders(order(123), order(234))
  }

}
