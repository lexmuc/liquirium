package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{typicalCurrenyPair, coinbaseTrade => ct}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.{dec, milli}
import io.liquirium.core.helpers.MarketHelpers.eid
import io.liquirium.core.{Side, StringTradeId, Trade}
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}

class CoinbaseModelConverterTest_Trade extends BasicTest {
  val converter = new CoinbaseModelConverter(eid(123))

  private def convert(pt: CoinbaseTrade) = converter.convertTrade(pt)

  private val typicalMarket = converter.getMarket(typicalCurrenyPair)

  private def afterConversionOf(ct: CoinbaseTrade)(a: Trade => Unit): Unit = {
    a(convert(ct.copy(side = Side.Buy)))
    a(convert(ct.copy(side = Side.Sell)))
  }

  test("the trade id is set as a string trade id and the order id as a string") {
    afterConversionOf(ct(tradeId = "555"))(_.id should equal(StringTradeId("555")))
    afterConversionOf(ct(orderId = "911"))(_.orderId shouldEqual Some("911"))
  }

  test("buys get the positive quantity, sells the negative quantity") {
    convert(ct(side = Side.Buy, size = dec("1.2"))).quantity shouldEqual dec("1.2")
    convert(ct(side = Side.Sell, size = dec("2.3"))).quantity shouldEqual dec("-2.3")
  }

  test("the market is set on the trade after being converted") {
    afterConversionOf(ct(productId = typicalCurrenyPair)) { _.market shouldEqual typicalMarket }
  }

  test("the price is simply transferred") {
    afterConversionOf(ct(price = dec("0.08")))(_.price shouldEqual 0.08)
  }

  //  test("the fee is built from coinbase product id and commission") {
  //    convert(ct(productId = "USDT-BTC", commission = dec("2.231")))
  //      .fees shouldEqual Seq(
  //      LedgerRef(typicalMarket.exchangeId, "USDT") -> dec("2.231")
  //    )
  //  }

  test("the coinbase trade time is used as time") {
    afterConversionOf(ct(tradeTime = milli(333))) { _.time shouldEqual milli(333) }
  }

}
