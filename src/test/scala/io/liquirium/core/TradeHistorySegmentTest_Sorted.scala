package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.sec
import io.liquirium.core.helper.TradeHelpers.trade

class TradeHistorySegmentTest_Sorted extends TradeHistorySegmentTest {

  test("trades should be first sorted by time, then by id") {
    val trades = List(
      trade(sec(10), "f"),
      trade(sec(5), "d"),
      trade(sec(2), "a"),
      trade(sec(2), "b"),
      trade(sec(5), "e"),
      trade(sec(2), "c"),
      trade(sec(10), "g"),
    )
    trades.sorted shouldEqual List(
      trade(sec(2), "a"),
      trade(sec(2), "b"),
      trade(sec(2), "c"),
      trade(sec(5), "d"),
      trade(sec(5), "e"),
      trade(sec(10), "f"),
      trade(sec(10), "g"),      
    )
  }

  test("correct sorting of both id strings and numbers ?") {
    
  }
}
