package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.helpers.{BasicTest, TradeHelpers}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class OrderSetOpsTest_RecordTrade extends BasicTest {
  import io.liquirium.core.OrderSetOps.OrderSetOps

  def trade(oid: Option[String] = None, amount: BigDecimal): Trade =
    TradeHelpers.trade(orderId = oid, quantity = amount)

  test("a sold quantity is deducted from the respective sell order") {
    Set(order(id = "s1", originalQuantity = dec("-4"), quantity = dec("-2"), price = dec("20")), order(234))
      .record(trade(Some("s1"), dec("-1.5"))) shouldEqual Set(
      order(id = "s1", quantity = dec("-0.5"), originalQuantity = dec("-4"), price = dec("20")),
      order(234)
    )
  }

  test("sell orders are removed when filled completely") {
    Set(order(id = "s1", originalQuantity = dec("-4"), quantity = dec("-2"), price = dec("20")), order(234))
      .record(trade(Some("s1"), dec("-2"))) shouldEqual Set(order(234))
  }

  test("a bought quantity is deducted from the respective buy order") {
    Set(order(id = "b1", originalQuantity = dec("4"), quantity = dec("2"), price = dec("20")), order(234))
      .record(trade(Some("b1"), dec("1.5"))) shouldEqual Set(
      order(id = "b1", quantity = dec("0.5"), originalQuantity = dec("4"), price = dec("20")),
      order(234)
    )
  }

  test("buy orders are removed when filled completely") {
    Set(order(id = "b1", originalQuantity = dec("4"), quantity = dec("2"), price = dec("20")), order(234))
      .record(trade(Some("b1"), dec("2"))) shouldEqual Set(order(234))
  }

  test("recording trades without order id has no effect") {
    Set(order(123), order(234)).record(trade(None, dec(2))) shouldEqual
      Set(order(123), order(234))
  }

}
