package io.liquirium.connect.coinbase

import io.liquirium.core.TradingPair
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CoinbaseModelConverterTest_ProductId extends CoinbaseModelConverterTest {

  private def convert(p: TradingPair) = converter().getProductId(p)

  test("the product id consists of base and quote connected with a '-'") {
    convert(TradingPair("IOT", "BTC")) shouldEqual "IOT-BTC"
  }

}
