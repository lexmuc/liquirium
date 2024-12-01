package io.liquirium.core

import io.liquirium.core.helpers.CandleHelpers.{c10, candle, candleHistorySegment, e10}
import io.liquirium.core.helpers.CoreHelpers.{asset, dec, sec, secs}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CandleHistorySegmentTest_LastPrice extends CandleHistorySegmentTest {

  test("it returns None if there are no candles") {
    val chs = candleHistorySegment(sec(10), secs(5))()
    chs.lastPrice shouldEqual None
  }

  test("it returns None if all candles are empty") {
    val chs = candleHistorySegment(
      e10(sec(100)),
      e10(sec(110)),
    )
    chs.lastPrice shouldEqual None
  }

  test("it returns the close price of the last candle if it is not empty") {
    val chs = candleHistorySegment(
      c10(sec(100), 1).copy(close = dec(123)),
      c10(sec(110), 1).copy(close = dec(234)),
    )
    chs.lastPrice shouldEqual Some(dec(234))
  }

  test("if there are empty candles at the end, it returns the close price of the last non-empty candle") {
    val chs = candleHistorySegment(
      c10(sec(100), 1).copy(close = dec(123)),
      c10(sec(110), 1).copy(close = dec(234)),
      e10(sec(120)),
      e10(sec(130)),
    )
    chs.lastPrice shouldEqual Some(dec(234))
  }

}
