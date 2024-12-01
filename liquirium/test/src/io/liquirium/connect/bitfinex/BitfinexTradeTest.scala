package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.trade
import io.liquirium.core.helpers.CoreHelpers.sec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BitfinexTradeTest extends BasicTest {

  test("the history id is the id converted to a string") {
    trade(id = 1234).historyId shouldEqual "1234"
  }

  test("the history timestamp is simply the timestamp") {
    trade(timestamp = sec(123)).historyTimestamp shouldEqual sec(123)
  }

}
