package io.liquirium.connect.binance

import io.liquirium.connect.binance.helpers.BinanceTestHelpers.candle
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.{milli, sec}

class BinanceCandleTest extends BasicTest {

  test("the history id is the timestamp in millis converted to a string") {
    candle(openTime = milli(123)).historyId shouldEqual "123"
  }

  test("the history timestamp is simply the timestamp") {
    candle(openTime = sec(123)).historyTimestamp shouldEqual sec(123)
  }

}
