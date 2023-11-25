package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.order
import io.liquirium.core.helpers.CoreHelpers.sec

class BitfinexOrderTest extends BasicTest {

  test("the history id is the id converted to a string") {
    order(id = 1234).historyId shouldEqual "1234"
  }

  test("the history timestamp is the update timestamp") {
    order(creationTimestamp = sec(123), updateTimestamp = sec(234)).historyTimestamp shouldEqual sec(234)
  }

}
