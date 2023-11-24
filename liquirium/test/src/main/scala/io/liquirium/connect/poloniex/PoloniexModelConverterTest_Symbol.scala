package io.liquirium.connect.poloniex

import io.liquirium.core.TradingPair
import io.liquirium.core.helpers.MarketHelpers.pair

class PoloniexModelConverterTest_Symbol extends PoloniexModelConverterTest {

  private def getSymbol(tp: TradingPair) = converter().getSymbol(tp)

  test("the symbol is constructed from the traiding pair in the correct form") {
    getSymbol(pair("BTC", "USDT")) shouldEqual "BTC_USDT"
  }

}
