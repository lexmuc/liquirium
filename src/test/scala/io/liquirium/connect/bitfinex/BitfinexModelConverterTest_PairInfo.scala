package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.pairInfo
import io.liquirium.core.{OrderQuantityPrecision, PricePrecision}

class BitfinexModelConverterTest_PairInfo extends BitfinexModelConverterTest {

  private def convert(bpi: BitfinexPairInfo) = converter().convertPairInfo(bpi)

  test("the price precision is set to 5 significant digits and 8 max decimals after point precision") {
    convert(pairInfo(1)).pricePrecision shouldEqual
      PricePrecision.SignificantDigits(
        numberOfDigits = 5,
        maxDecimalsAfterPointPrecision = Some(PricePrecision.digitsAfterSeparator(8)),
      )
  }

  test("the order quantity precision is set to 8 digits after seperator") {
    convert(pairInfo(1)).orderQuantityPrecision shouldEqual
      OrderQuantityPrecision.DigitsAfterSeparator(8)
  }

}
