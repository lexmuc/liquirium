package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.symbolInfo
import io.liquirium.core.{OrderQuantityPrecision, PricePrecision}

class PoloniexModelConverterTest_SymbolInfo extends PoloniexModelConverterTest {

  private def convert(psi: PoloniexSymbolInfo) = converter().convertSymbolInfo(psi)

  test("the price scale is set as the price precision") {
    convert(symbolInfo(priceScale = 5)).pricePrecision shouldEqual PricePrecision.digitsAfterSeparator(5)
  }

  test("the quantity scale is set as the order quantity precision") {
    convert(symbolInfo(quantityScale = 6)).orderQuantityPrecision shouldEqual OrderQuantityPrecision.DigitsAfterSeparator(6)
  }

}