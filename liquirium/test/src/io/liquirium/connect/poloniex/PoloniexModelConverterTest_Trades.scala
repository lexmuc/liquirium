package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.{poloniexTrade => pt, typicalCurrencyPair => typicalPair}
import io.liquirium.core.helpers.CoreHelpers.{dec, milli}
import io.liquirium.core.{LedgerRef, Side, StringTradeId, Trade}
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}

//noinspection RedundantDefaultArgument
class PoloniexModelConverterTest_Trades extends PoloniexModelConverterTest {

  private def convert(pt: PoloniexTrade) = converter().convertTrade(pt)

  private val typicalMarket = converter().getMarket(typicalPair)

  private def afterConversionOf(pt: PoloniexTrade)(a: Trade => Unit): Unit = {
    a(convert(pt.copy(side = Side.Buy)))
    a(convert(pt.copy(side = Side.Sell)))
  }

  test("the id is converted to a string trade id and the order id are simply transferred") {
    afterConversionOf(pt(id = "555"))(_.id should equal(StringTradeId("555")))
    afterConversionOf(pt(orderId = "911"))(_.orderId shouldEqual Some("911"))
  }

  test("buys get the positive quantity, sells the negative quantity") {
    convert(pt(side = Side.Buy, quantity = dec("1.2"))).quantity shouldEqual dec("1.2")
    convert(pt(side = Side.Sell, quantity = dec("2.3"))).quantity shouldEqual dec("-2.3")
  }

  test("the market is set on the trade after being converted") {
    afterConversionOf(pt(symbol = typicalPair)) { _.market shouldEqual typicalMarket }
  }

  test("the price is simply transferred") {
    afterConversionOf(pt(price = BigDecimal("0.08")))(_.price shouldEqual 0.08)
  }

  test("the fee is built from poloniex feeCurrency and feeAmount") {
    convert(pt(feeCurrency = "USDT", feeAmount = BigDecimal("2.231")))
      .fees shouldEqual Seq(
      LedgerRef(typicalMarket.exchangeId, "USDT") -> BigDecimal(2.231)
    )
  }

  test("the poloniex createTime is used as time") {
    afterConversionOf(pt(createTime = milli(333))) { _.time shouldEqual milli(333) }
  }

}
