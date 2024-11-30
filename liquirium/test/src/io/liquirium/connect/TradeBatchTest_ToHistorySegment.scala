package io.liquirium.connect

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeBatch, tradeHistorySegment}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class TradeBatchTest_ToHistorySegment extends BasicTest {

  test("it can be turned into a trade history segment if empty") {
    tradeBatch(sec(10))().toHistorySegment shouldEqual tradeHistorySegment(sec(10))()
  }

  test("it can be turned into a trade history segment if it contains trades") {
    tradeBatch(sec(10))(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    ).toHistorySegment shouldEqual tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    )
  }

}
