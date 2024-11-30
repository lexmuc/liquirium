package io.liquirium.core

import io.liquirium.bot.helpers.OperationRequestHelpers.{cancelRequest, orderRequest}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.helpers.OperationIntentHelpers.{cancelIntent, orderIntent}
import io.liquirium.core.helpers.OrderHelpers.modifiers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class OperationIntentConverterTest extends BasicTest {

  def converter(market: Market, modifiers: Set[OrderModifier]): OperationIntentConverter =
    OperationIntentConverter(market, modifiers)

  test("it converts cancel and order intents to real requests with the given parameters") {
    converter(m(123), modifiers(234)).apply(Seq(cancelIntent("ABC"), orderIntent(dec(123) -> dec(234)))) shouldEqual
      Seq(
        cancelRequest("ABC", m(123)),
        orderRequest(m(123), quantity = dec(234), price = dec(123), modifiers = modifiers(234))
      )
  }

}
