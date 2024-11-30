package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.{dec, milli, sec}
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment => segment}
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}

class TradeHistorySegmentTest_GetExtensionTo extends TradeHistorySegmentTest {

  test("the extension of the empty segment to itself is the empty segment") {
    val s1 = segment(sec(10))()
    s1.getExtensionTo(s1) shouldEqual s1
  }

  test("the extension of a non-empty segment to itself contains exactly the last milliseconds trades") {
    val s1 = segment(milli(10))(
      trade(milli(11), "1"),
      trade(milli(22), "2_1"),
      trade(milli(22), "2_2"),
    )
    s1.getExtensionTo(s1) shouldEqual segment(milli(22))(
      trade(milli(22), "2_1"),
      trade(milli(22), "2_2"),
    )
  }

  test("if the other is a descendant the result contains the end instant trades and all new trades") {
    val s1 = segment(sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2_1"),
      trade(sec(2), "2_2"),
    )
    val s2 = s1.append(trade(sec(3), "3"))

    s1.getExtensionTo(s2) shouldEqual segment(sec(2))(
      trade(sec(2), "2_1"),
      trade(sec(2), "2_2"),
      trade(sec(3), "3"),
    )
  }

  test("in general the extension contains the last instant trades of the latest common ancestor plus new trades") {
    val s1 = segment(sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2_1"),
      trade(sec(2), "2_2"),
    )
    val s2 = s1.append(trade(sec(3), "3"))
    val s3 = s1.append(trade(sec(4), "4"))

    s2.getExtensionTo(s3) shouldEqual segment(sec(2))(
      trade(sec(2), "2_1"),
      trade(sec(2), "2_2"),
      trade(sec(4), "4"),
    )
  }

  test("additional trades in the latest common ancestor's second are included") {
    val s1 = segment(sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2_1"),
    )
    val s2 = s1.append(trade(sec(3), "3"))
    val s3 = s1.append(trade(sec(2), "2_2")).append(trade(sec(4), "4"))

    s2.getExtensionTo(s3) shouldEqual segment(sec(2))(
      trade(sec(2), "2_1"),
      trade(sec(2), "2_2"),
      trade(sec(4), "4"),
    )
  }

  test("the extension may cause trades to disappear in the latest common ancestor's last instant") {
    val s1 = segment(sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2_1"),
    )
    val s2 = s1.append(trade(sec(2), "2_2")).append(trade(sec(3), "3"))
    val s3 = s1.append(trade(sec(4), "4"))

    s2.getExtensionTo(s3) shouldEqual segment(sec(2))(
      trade(sec(2), "2_1"),
      trade(sec(4), "4"),
    )
  }

  test("the extension may cause trades to update in the latest common ancestor's last instant") {
    val s1 = segment(sec(0))(
      trade(sec(1), "1"),
      trade(sec(2), "2_1"),
    )
    val s2 = s1.append(trade(sec(2), "2_2")).append(trade(sec(3), "3"))
    val s3 = s1.append(trade(sec(2), "2_2").copy(price = dec(888))).append(trade(sec(4), "4"))

    s2.getExtensionTo(s3) shouldEqual segment(sec(2))(
      trade(sec(2), "2_1"),
      trade(sec(2), "2_2").copy(price = dec(888)),
      trade(sec(4), "4"),
    )
  }

  test("extensions that only remove trades can be obtained") {
    val s1 = segment(sec(0))(
      trade(sec(1), "1"),
    )
    val s2 = s1.append(trade(sec(2), "2_1"))
    s2.getExtensionTo(s1) shouldEqual segment(sec(1))(
      trade(sec(1), "1"),
    )
  }

  test("an exception is thrown when the segments have different starts") {
    val s1 = segment(sec(0))(
      trade(sec(1), "1"),
    )
    val s2 = segment(sec(1))(
      trade(sec(1), "1"),
    )
    an[Exception] shouldBe thrownBy(s1.getExtensionTo(s2))
  }

}