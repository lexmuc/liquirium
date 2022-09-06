package io.liquirium.connect

import io.liquirium.core.CandleHistorySegment
import io.liquirium.core.helper.BasicTest
import io.liquirium.core.helper.CandleHelpers.{c10, c5, forwardCandleBatch}
import io.liquirium.core.helper.CoreHelpers.{sec, secs}

class ForwardCandleBatchTest extends BasicTest {

  test("it can be turned into a candle history segment when there is no next batch start") {
    val batchCandles = List(c5(sec(15), 1), c5(sec(25), 1))
    forwardCandleBatch(
      start = sec(10),
      resolution = secs(5),
      candles = batchCandles,
      nextBatchStart = None,
    ).toHistorySegment shouldEqual CandleHistorySegment.fromForwardCandles(
      start = sec(10),
      resolution = secs(5),
      candles = batchCandles,
    )
  }

  test("when there is a next batch start this will become the end of the history segment") {
    val batchCandles = List(c5(sec(15), 1), c5(sec(25), 1))
    forwardCandleBatch(
      start = sec(10),
      resolution = secs(5),
      candles = batchCandles,
      nextBatchStart = Some(sec(40)),
    ).toHistorySegment shouldEqual CandleHistorySegment.fromForwardCandles(
      start = sec(10),
      resolution = secs(5),
      candles = batchCandles,
      end = Some(sec(40)),
    )
  }

  test("it may contain gaps") {
    forwardCandleBatch(
      start = sec(10),
      resolution = secs(5),
      candles = List(c5(sec(15), 1), c5(sec(25), 1)),
    )
  }

  test("it throws an exception when candles are not properly ordered") {
    an[Exception] shouldBe thrownBy(forwardCandleBatch(
      start = sec(10),
      resolution = secs(5),
      candles = List(c5(sec(15), 1), c5(sec(25), 1), c5(sec(20), 1)),
    ))
  }

  test("it throws an exception when it includes candles before the start") {
    an[Exception] shouldBe thrownBy(forwardCandleBatch(
      start = sec(20),
      resolution = secs(5),
      candles = List(c5(sec(15), 1), c5(sec(20), 1)),
    ))
  }

  test("it throws an exception when it includes candles after the next batch start") {
    an[Exception] shouldBe thrownBy(forwardCandleBatch(
      start = sec(10),
      resolution = secs(5),
      candles = List(c5(sec(10), 1), c5(sec(25), 1)),
      nextBatchStart = Some(sec(25)),
    ))
  }

  test("it throws an exception when it includes candles with a different resolution") {
    an[Exception] shouldBe thrownBy(forwardCandleBatch(
      start = sec(10),
      resolution = secs(5),
      candles = List(c5(sec(10), 1), c10(sec(15), 1)),
    ))
  }

  test("it throws an exception when a candle is not properly aligned") {
    an[Exception] shouldBe thrownBy(forwardCandleBatch(
      start = sec(10),
      resolution = secs(5),
      candles = List(c5(sec(10), 1), c5(sec(16), 1)),
    ))
  }

  test("it throws an exception when the next batch start is not properly aligned") {
    an[Exception] shouldBe thrownBy {
      forwardCandleBatch(
        start = sec(10),
        resolution = secs(5),
        candles = List(c5(sec(10), 1)),
        nextBatchStart = Some(sec(16)),
      )
    }
  }

  test("it throws an exception when the next batch start is earlier than the start") {
    an[Exception] shouldBe thrownBy {
      forwardCandleBatch(
        start = sec(10),
        resolution = secs(5),
        candles = List(),
        nextBatchStart = Some(sec(5)),
      )
    }
  }

  test("it throws an exception when the next batch start is equal to the start") {
    an[Exception] shouldBe thrownBy {
      forwardCandleBatch(
        start = sec(10),
        resolution = secs(5),
        candles = List(),
        nextBatchStart = Some(sec(10)),
      )
    }
  }

}
