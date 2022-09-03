package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.{coinbaseOrder => co}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec

class CoinbaseOrderTest extends BasicTest {

  test("the open quantity is the full quantity minus the filled quantity") {
    co(fullQuantity = dec("2.34"), filledQuantity = dec("1.11")).openQuantity shouldEqual 1.23
  }
}
