package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex
import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.milli
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class TradeHistoryTest extends BasicTest {

  private def t(id: Long, m: Long) = BitfinexTestHelpers.tradeWithTimestamp(id, milli(m))

  test("start date and trades are not changed if it is before the old start date") {
    TradeHistory(Seq(t(1, 1001), t(2, 1002)), milli(1000)).dropBefore(milli(800)) shouldEqual
      bitfinex.TradeHistory(Seq(t(1, 1001), t(2, 1002)), milli(1000))
  }

  test("old trades are dropped if they exist and the start date is updated") {
    bitfinex.TradeHistory(Seq(t(1, 1001), t(3, 1003)), milli(1000)).dropBefore(milli(1002)) shouldEqual
      bitfinex.TradeHistory(Seq(t(3, 1003)), milli(1002))
  }

  test("the start date is not advanced past the last trade timestamp and the respective trades are not dropped") {
    bitfinex.TradeHistory(Seq(t(1, 1001), t(21, 1002), t(22, 1002)), milli(1000)).dropBefore(milli(1003)) shouldEqual
      bitfinex.TradeHistory(Seq(t(21, 1002), t(22, 1002)), milli(1002))
  }

  test("the start is not moved passed the old when there are no trades") {
    bitfinex.TradeHistory(Seq(), milli(1000)).dropBefore(milli(2000)) shouldEqual bitfinex.TradeHistory(Seq(), milli(1000))
  }

  test("new trades can be appended") {
    val t3 = t(3, 1003)
    val t4 = t(4, 1004)
    bitfinex.TradeHistory(Seq(t(1, 1001), t(2, 1002)), milli(1000)).append(Seq(t3, t4)) shouldEqual
      bitfinex.TradeHistory(Seq(t(1, 1001), t(2, 1002), t3, t4), milli(1000))
  }

}
