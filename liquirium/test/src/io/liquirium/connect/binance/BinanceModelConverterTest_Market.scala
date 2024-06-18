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
    convert("MANANGN").tradingPair shouldEqual TradingPair("MANA", "NGN")
    convert("REPUSDT").tradingPair shouldEqual TradingPair("REP", "USDT")
    convert("REPTUSD").tradingPair shouldEqual TradingPair("REP", "TUSD")
    convert("REPUSDC").tradingPair shouldEqual TradingPair("REP", "USDC")
    convert("REPUSDS").tradingPair shouldEqual TradingPair("REP", "USDS")
    convert("MANAETH").tradingPair shouldEqual TradingPair("MANA", "ETH")
    convert("MANABNB").tradingPair shouldEqual TradingPair("MANA", "BNB")
    convert("MANAPAX").tradingPair shouldEqual TradingPair("MANA", "PAX")
    convert("HNTBUSD").tradingPair shouldEqual TradingPair("HNT", "BUSD")
    convert("HNTFDUSD").tradingPair shouldEqual TradingPair("HNT", "FDUSD")
    convert("USDTMXN").tradingPair shouldEqual TradingPair("USDT", "MXN")
    an[Exception] shouldBe thrownBy(convert("AAAAXXX"))
  }

  test("for usdt and other short symbols even a six-letter symbol may be unevenly split") {
    convert("SCUSDT").tradingPair shouldEqual TradingPair("SC", "USDT")
  }

}
