package io.liquirium.core

import io.liquirium.core.helper.CoreHelpers.{sec, secs}

class CandleHistorySegmentTest_Basics extends CandleHistorySegmentTest {

  test("it can be created empty only with start and resolution") {
    val chs = empty(sec(10), secs(5))
    chs.start shouldEqual sec(10)
    chs.resolution shouldEqual secs(5)
    chs.reverseCandles shouldEqual List()
  }

  test("the end of an empty segment is equal to the start") {
    empty(start = sec(10), resolution = secs(5)).end shouldEqual sec(10)
  }

}
