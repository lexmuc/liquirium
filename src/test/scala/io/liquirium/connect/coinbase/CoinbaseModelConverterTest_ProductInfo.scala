package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.productInfo
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.{OrderConstraints, OrderQuantityPrecision, PricePrecision}

class CoinbaseModelConverterTest_ProductInfo extends CoinbaseModelConverterTest {

  private def convert(pi: CoinbaseProductInfo): OrderConstraints = converter().convertProductInfo(pi)

  test("the quote increment is set as the price precision") {
    convert(productInfo(quoteIncrement = dec("0.001"))).pricePrecision shouldEqual PricePrecision.MultipleOf.apply(step = dec("0.001"))
  }

  test("the base increment is set as the order quantity precision") {
    convert(productInfo(baseIncrement = dec("0.0001"))).orderQuantityPrecision shouldEqual OrderQuantityPrecision.MultipleOf(dec("0.0001"))
  }

}
