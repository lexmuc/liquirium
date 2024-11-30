package io.liquirium.connect

import io.liquirium.core.CandleHistorySegment
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.{c5, candleBatch, candleHistorySegment, e5}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CandleBatchTest_ToHistorySegment extends BasicTest {

  test("it has the proper start and candleLength when empty") {
    candleBatch(start = sec(10), candleLength = secs(5), nextBatchStart = None)()
      .toHistorySegment shouldEqual CandleHistorySegment.empty(sec(10), secs(5))
  }

  test("it can be turned into a candle history segment when there is no next batch start") {
    candleBatch(start = sec(10), candleLength = secs(5), nextBatchStart = None)(
      c5(sec(10), 1),
      c5(sec(15), 1),
    ).toHistorySegment shouldEqual candleHistorySegment(
      c5(sec(10), 1),
      c5(sec(15), 1),
    )
  }

  test("when there is a next batch start the history segment is padded until this start") {
    candleBatch(start = sec(10), candleLength = secs(5), nextBatchStart = Some(sec(30)))(
      c5(sec(10), 1),
      c5(sec(15), 1),
    ).toHistorySegment shouldEqual candleHistorySegment(
      c5(sec(10), 1),
      c5(sec(15), 1),
      e5(sec(20)),
      e5(sec(25)),
    )
  }

  test("gaps are filled with empty candles") {
    candleBatch(start = sec(10), candleLength = secs(5))(
      c5(sec(15), 1),
      c5(sec(30), 1),
    ).toHistorySegment shouldEqual candleHistorySegment(
      e5(sec(10)),
      c5(sec(15), 1),
      e5(sec(20)),
      e5(sec(25)),
      c5(sec(30), 1),
    )
  }

}
