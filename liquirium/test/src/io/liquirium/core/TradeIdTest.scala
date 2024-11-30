package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class TradeIdTest extends BasicTest {

  private def sid(s: String) = StringTradeId(s)

  test("converting a StringTradeId to a String yields the value") {
    sid("asdf").toString shouldEqual "asdf"
  }

  test("ordering string trade ids orders them by the string value (ascending)") {
    List(sid("x"), sid("z"), sid("y")).sorted(Ordering[TradeId]) shouldEqual List(sid("x"), sid("y"), sid("z"))
  }

}
