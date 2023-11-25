package io.liquirium.core

import io.liquirium.core.helpers.BasicTest

class TradingPairTest extends BasicTest {

  test("a pair can be flipped") {
    TradingPair("A", "B").flip shouldEqual TradingPair("B", "A")
  }

}
