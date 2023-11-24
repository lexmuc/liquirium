package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.{ExchangeId, TradingPair}

class BitfinexModelConverterTest_Symbol extends BasicTest {

  private def convert(s: String, eid: ExchangeId = ExchangeId("X")) = new BitfinexModelConverter(eid).getMarket(s)

  private def convert(p: TradingPair) = new BitfinexModelConverter(ExchangeId("X")).getSymbol(p)

  test("the exchange id is set to the converter exchange id") {
    convert("tIOTBTC", ExchangeId("asdf")).exchangeId shouldEqual ExchangeId("asdf")
  }

  test("the trading pair is obtained by splitting the symbol into base and quote and removing the 't'-prefix") {
    convert("tIOTBTC").tradingPair shouldEqual TradingPair("IOT", "BTC")
  }

  test("funding markets are transformed to a FUNDING-[BaseAsset] market") {
    convert("fIOT").tradingPair shouldEqual TradingPair("FUNDING", "IOT")
  }

  test("derivatives pairs are properly transformed") {
    convert("tBTCF0:USTF0").tradingPair shouldEqual TradingPair("BTCF0", "USTF0")
    convert("tBTCF0:USTF0").tradingPair shouldEqual TradingPair("BTCF0", "USTF0")
  }

  test("weird symbols are transformed to a ???-??? pair") {
    convert("xxx").tradingPair shouldEqual TradingPair("???", "???")
    convert("fIOTXX").tradingPair shouldEqual TradingPair("???", "???")
    convert("tIOT").tradingPair shouldEqual TradingPair("???", "???")
  }

  test("for normal pairs the symbol is obtained by combining base and quote and adding the prefix 't'") {
    convert(TradingPair("IOT", "BTC")) shouldEqual "tIOTBTC"
  }

  test("for derivative pairs the symbol is obtained by adding an additional colon") {
    convert(TradingPair("BTCF0", "USTF0")) shouldEqual "tBTCF0:USTF0"
  }

}
