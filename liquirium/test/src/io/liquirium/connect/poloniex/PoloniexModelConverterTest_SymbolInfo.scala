package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.symbolInfo
import io.liquirium.util.NumberPrecision
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class PoloniexModelConverterTest_SymbolInfo extends PoloniexModelConverterTest {

  private def convert(psi: PoloniexSymbolInfo) = converter().convertSymbolInfo(psi)

  test("the price scale is set as the price precision") {
    convert(symbolInfo(priceScale = 5)).pricePrecision shouldEqual NumberPrecision.digitsAfterSeparator(5)
  }

  test("the quantity scale is set as the order quantity precision") {
    convert(symbolInfo(quantityScale = 6)).quantityPrecision shouldEqual NumberPrecision.digitsAfterSeparator(6)
  }

}
