package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.poloniexCandle
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.milli

class PoloniexCandleTest extends BasicTest {

  test("the history id is the timestamp as milliseconds") {
    poloniexCandle(startTime = milli(123)).historyId shouldEqual "123"
  }

  test("the history timestamp is the start") {
    poloniexCandle(startTime = milli(123)).historyTimestamp shouldEqual milli(123)
  }

}



