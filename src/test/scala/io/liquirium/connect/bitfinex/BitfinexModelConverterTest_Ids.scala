package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.MarketHelpers.eid

class BitfinexModelConverterTest_Ids extends BasicTest {

  test("an order is is simply converted to a string") {
    new BitfinexModelConverter(eid(0)).genericOrderId(1234) shouldEqual "1234"
  }

}
