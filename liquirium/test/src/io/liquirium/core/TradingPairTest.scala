package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class TradingPairTest extends BasicTest {

  test("a pair can be flipped") {
    TradingPair("A", "B").flip shouldEqual TradingPair("B", "A")
  }

}
