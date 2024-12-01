package io.liquirium.connect

import io.liquirium.core.helpers.CandleHelpers.candleHistorySegment
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CandleUpdateOverlapStrategyTest extends BasicTest {

  test("the complete strategy always returns the segment start") {
    val strategy = CandleUpdateOverlapStrategy.complete
    val segment = candleHistorySegment(sec(10), secs(5))(1, 2, 3, 4)
    strategy.apply(segment) shouldEqual sec(10)
  }

  test("the number of candles strategy yields a start the given number of candle lengths before the segment end") {
    val strategy = CandleUpdateOverlapStrategy.numberOfCandles(3)
    val segment = candleHistorySegment(sec(10), secs(5))(1, 2, 3, 4)
    strategy.apply(segment) shouldEqual sec(15)
  }

  test("the start returned by the number of candles strategy is at least the segment start") {
    val strategy = CandleUpdateOverlapStrategy.numberOfCandles(5)
    val segment = candleHistorySegment(sec(10), secs(5))(1, 2, 3, 4)
    strategy.apply(segment) shouldEqual sec(10)
  }

}
