package io.liquirium.connect.bitfinex

import io.liquirium.core.helpers.BasicTest
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.candle
import io.liquirium.core.helpers.CoreHelpers.{milli, sec}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BitfinexCandleTest extends BasicTest {

  test("the history id is the timestamp in millis converted to a string") {
    candle(timestamp = milli(123)).historyId shouldEqual "123"
  }

  test("the history timestamp is simply the timestamp") {
    candle(timestamp = sec(123)).historyTimestamp shouldEqual sec(123)
  }

}
