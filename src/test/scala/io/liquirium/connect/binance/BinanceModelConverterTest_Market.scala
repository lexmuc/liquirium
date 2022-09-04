package io.liquirium.connect.binance

import io.liquirium.core.{ExchangeId, Market, TradingPair}

class BinanceModelConverterTest_Market extends BinanceModelConverterTest {

  def convert(s: String, eid: ExchangeId = ExchangeId("X")): Market = converter(eid).getMarket(s)

  test("the exchange id is set to the converter exchange id") {
    convert("IOTBTC", ExchangeId("asdf")).exchangeId shouldEqual ExchangeId("asdf")
  }

  test("the base and quote currency are assigned in this order") {
    convert("IOTBTC").tradingPair shouldEqual TradingPair("IOT", "BTC")
  }

  test("there may be symbols longer than 6 characters when the quote currency is supported (error otherwise)") {
    convert("MANABTC").tradingPair shouldEqual TradingPair("MANA", "BTC")
    convert("REPUSDT").tradingPair shouldEqual TradingPair("REP", "USDT")
    convert("MANAETH").tradingPair shouldEqual TradingPair("MANA", "ETH")
    convert("MANABNB").tradingPair shouldEqual TradingPair("MANA", "BNB")
    an[Exception] shouldBe thrownBy(convert("AAAAXXX"))
  }

}
