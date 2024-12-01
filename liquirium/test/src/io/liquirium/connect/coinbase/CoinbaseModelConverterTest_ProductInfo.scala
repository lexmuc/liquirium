package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.productInfo
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.OrderConstraints
import io.liquirium.util.NumberPrecision
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CoinbaseModelConverterTest_ProductInfo extends CoinbaseModelConverterTest {

  private def convert(pi: CoinbaseProductInfo): OrderConstraints = converter().convertProductInfo(pi)

  test("the quote increment is set as the price precision") {
    convert(productInfo(quoteIncrement = dec("0.001"))).pricePrecision shouldEqual
      NumberPrecision.MultipleOf.apply(step = dec("0.001"))
  }

  test("the base increment is set as the order quantity precision") {
    convert(productInfo(baseIncrement = dec("0.0001"))).quantityPrecision shouldEqual
      NumberPrecision.multipleOf(dec("0.0001"))
  }

}
