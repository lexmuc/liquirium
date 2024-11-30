package io.liquirium.connect.binance

import io.liquirium.core.TradingPair
import io.liquirium.core.helpers.MarketHelpers.pair
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BinanceModelConverterTest_Symbol extends BinanceModelConverterTest {

  def convert(pair: TradingPair): String = converter().getSymbol(pair)

  test("it simply concatenates base and quote asset codes") {
    convert(pair("ASDF", "JK")) shouldEqual "ASDFJK"
  }

}
