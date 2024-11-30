package io.liquirium.connect

import io.liquirium.core.helpers.{BasicTest, TradeHelpers}
import io.liquirium.core.helpers.CoreHelpers.{milli, millis}
import io.liquirium.core.helpers.TradeHelpers.trade
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class TradeUpdateOverlapStrategyTest extends BasicTest {

  test("the complete strategy always returns the segment start") {
    val strategy = TradeUpdateOverlapStrategy.complete
    val segment = TradeHelpers.tradeHistorySegment(milli(1000))(
      trade(milli(1500), "A"),
      trade(milli(2000), "B"),
      trade(milli(3000), "C"),
    )
    strategy.apply(segment) shouldEqual milli(1000)
  }

  test("the fixed overlap strategy yields the instant earlier than the last segment trade by the given duration") {
    val segment = TradeHelpers.tradeHistorySegment(milli(1000))(
      trade(milli(1500), "A"),
      trade(milli(2000), "B"),
      trade(milli(3000), "C"),
    )
    val strategy = TradeUpdateOverlapStrategy.fixedOverlap(millis(1234))
    strategy.apply(segment) shouldEqual milli(1766)
  }

  test("the fixed overlap strategy yields at least the start of the segment, not earlier start") {
    val segment = TradeHelpers.tradeHistorySegment(milli(1000))(
      trade(milli(1500), "A"),
      trade(milli(2000), "B"),
      trade(milli(3000), "C"),
    )
    val strategy = TradeUpdateOverlapStrategy.fixedOverlap(millis(2001))
    strategy.apply(segment) shouldEqual milli(1000)
  }

}
