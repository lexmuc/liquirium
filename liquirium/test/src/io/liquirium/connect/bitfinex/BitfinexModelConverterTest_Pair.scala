package io.liquirium.connect.bitfinex

import io.liquirium.core.{ExchangeId, TradingPair}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BitfinexModelConverterTest_Pair extends BitfinexModelConverterTest {

  private def convert(pair: String, eid: ExchangeId = ExchangeId("X")) = new BitfinexModelConverter(eid).getMarketFromPair(pair)

  test("the exchange id is set to the converter exchange id") {
    convert("AAABBB", ExchangeId("asdf")).exchangeId shouldEqual ExchangeId("asdf")
  }

  test("the trading pair is obtained by splitting the pair in the middle into base and quote in case of 6 letters") {
    convert("AAABBB").tradingPair shouldEqual TradingPair("AAA", "BBB")
  }

  test("the trading pair is obtained by splitting the pair into base and quote at the : if it contains one") {
    convert("AAAA:BBB").tradingPair shouldEqual TradingPair("AAAA", "BBB")
    convert("AA:BBB").tradingPair shouldEqual TradingPair("AA", "BBB")
  }

  test("pairs with no : and other than 6 letters are transformed to a ???-??? trading pair") {
    convert("AAAABBB").tradingPair shouldEqual TradingPair("???", "???")
  }

}
