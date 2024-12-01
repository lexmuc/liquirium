package io.liquirium.connect.coinbase

import io.liquirium.core.{ExchangeId, Market, TradingPair}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CoinbaseModelConverterTest_Market extends CoinbaseModelConverterTest {

  test("base and quote are extracted from the product id and the exchange id is set") {
    converter(ExchangeId("x")).getMarket("BTC-USD") shouldEqual Market(ExchangeId("x"), TradingPair("BTC", "USD"))
  }
}
