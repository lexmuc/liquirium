package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.pairInfo
import io.liquirium.util.NumberPrecision
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BitfinexModelConverterTest_PairInfo extends BitfinexModelConverterTest {

  private def convert(bpi: BitfinexPairInfo) = converter().convertPairInfo(bpi)

  test("the price precision is set to 5 significant digits and 8 max decimals after point precision") {
    convert(pairInfo(1)).pricePrecision shouldEqual
      NumberPrecision.SignificantDigits(
        numberOfDigits = 5,
        maxDecimalsAfterPoint = Some(8),
      )
  }

  test("the order quantity precision is set to 8 digits after separator") {
    convert(pairInfo(1)).quantityPrecision shouldEqual
      NumberPrecision.digitsAfterSeparator(8)
  }

}
