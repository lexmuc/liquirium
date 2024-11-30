package io.liquirium.connect.binance

import io.liquirium.connect.binance.helpers.BinanceTestHelpers.symbolInfo
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.util.NumberPrecision
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BinanceModelConverterTest_SymbolInfo extends BinanceModelConverterTest {

  private def convert(si: BinanceSymbolInfo) = converter().convertSymbolInfo(si)

  test("the tick size is set as the price precision") {
    convert(symbolInfo(tickSize = dec("0.001"))).pricePrecision shouldEqual
      NumberPrecision.MultipleOf.apply(step = dec("0.001"))
  }

  test("the step size is set as the order quantity precision") {
    convert(symbolInfo(stepSize = dec("0.0001"))).quantityPrecision shouldEqual
      NumberPrecision.multipleOf(dec("0.0001"))
  }

}
