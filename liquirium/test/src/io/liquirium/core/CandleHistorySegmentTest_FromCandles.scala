package io.liquirium.core

import io.liquirium.core.helpers.CandleHelpers.c10
import io.liquirium.core.helpers.CoreHelpers.sec
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}

class CandleHistorySegmentTest_FromCandles extends CandleHistorySegmentTest {

  test("it raises an exception when trying to create it from an empty iterable") {
    an[Exception] shouldBe thrownBy(fromCandles())
  }

  test("it can be created from a sorted candle iterable and exposes candles in the same order") {
    val chs = fromCandles(
      c10(sec(10), 1),
      c10(sec(20), 2),
    )
    chs shouldEqual List(
      c10(sec(10), 1),
      c10(sec(20), 2),
    )
  }

}
