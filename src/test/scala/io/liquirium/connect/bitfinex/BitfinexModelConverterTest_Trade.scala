package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.{trade => bitfinexTrade}
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.{BasicTest, MarketHelpers}
import io.liquirium.core.helpers.MarketHelpers.eid
import io.liquirium.core.{ExchangeId, LedgerRef, Market, StringTradeId, TradingPair}

class BitfinexModelConverterTest_Trade extends BasicTest {

  private def convert(t: BitfinexTrade, eid: ExchangeId = MarketHelpers.eid(0)) =
    new BitfinexModelConverter(eid).convertTrade(t)

  test("the id is converted to a string trade id") {
    convert(bitfinexTrade(id = 123)).id shouldEqual StringTradeId("123")
  }

  test("the order id is set to the input order id") {
    convert(bitfinexTrade(orderId = 456)).orderId shouldEqual Some("456")
  }

  test("the amount is simply set on the exact trade") {
    convert(bitfinexTrade(amount = dec("1.23"))).quantity shouldEqual dec("1.23")
    convert(bitfinexTrade(amount = dec("-1.23"))).quantity shouldEqual dec("-1.23")
  }

  test("the price is simply transferred") {
    convert(bitfinexTrade(price = dec("3.45"))).price shouldEqual dec("3.45")
  }

  test("no fee is added when the fee value is zero") {
    convert(bitfinexTrade(fee = dec("0"))).fees shouldEqual Seq()
  }

  test("a non-zero fee has the given currency (with the exchange id from the market), negative fee becomes positive!") {
    convert(bitfinexTrade(fee = dec("1.2"), feeCurrency = "ABC"), eid(123)).fees shouldEqual Seq(
      LedgerRef(eid(123), "ABC") -> dec("-1.2")
    )
    convert(bitfinexTrade(fee = dec("-1.2"), feeCurrency = "ABC"), eid(123)).fees shouldEqual Seq(
      LedgerRef(eid(123), "ABC") -> dec("1.2")
    )
  }

  test("the market is derived from the pair") {
    convert(bitfinexTrade(symbol = "tIOTBTC"), ExchangeId("E123")).market shouldEqual
      Market(ExchangeId("E123"), TradingPair("IOT", "BTC"))
  }

  test("the timestamp is simply transferred") {
    convert(bitfinexTrade(timestamp = sec(123))).time shouldEqual sec(123)
  }

}
