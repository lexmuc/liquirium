package io.liquirium.core

import io.liquirium.core.OperationIntent.{CancelIntent, OrderIntent}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.helpers.OperationIntentHelpers.orderIntent
import io.liquirium.core.helpers.OrderHelpers.modifiers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class OperationIntentTest extends BasicTest {

  test("a cancel intent can be converted to a trade request with a market") {
    CancelIntent("abc").toOperationRequest(m(123)) shouldEqual CancelRequest(m(123), "abc")
  }

  test("an order intent can be converted to an order request given a market and order modifiers") {
    OrderIntent(quantity = dec(12), price = dec(34)).toOperationRequest(m(12), modifiers(34)) shouldEqual
      OrderRequest(m(12), quantity = dec(12), price = dec(34), modifiers = modifiers(34))
  }

  test("the order intent volume is the (absolute) volume") {
    orderIntent(dec(2) -> dec(3)).volume shouldEqual dec(6)
    orderIntent(dec(2) -> dec(-3)).volume shouldEqual dec(6)
  }

}
