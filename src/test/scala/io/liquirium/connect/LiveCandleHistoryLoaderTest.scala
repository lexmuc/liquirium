package io.liquirium.connect

import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment, e5}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{Candle, CandleHistorySegment}

import java.time.{Duration, Instant}
import scala.concurrent.Future
import scala.util.Failure

class LiveCandleHistoryLoaderTest extends AsyncTestWithControlledTime with TestWithMocks {

  private val batchLoader =
    new FutureServiceMock[Instant => Future[CandleBatch], CandleBatch](_.apply(*))

  private var candleLength = secs(5)

  private var resultFuture: Future[CandleHistorySegment] = _

  private def loadSegment(start: Instant, time: Instant): Unit = {
    val segmentLoader = new LiveCandleHistoryLoader(
      batchLoader = batchLoader.instance,
      candleLength = candleLength,
    )
    resultFuture = segmentLoader.load(start, time)
  }

  private def returnBatch(
    start: Instant,
    nextStart: Option[Instant],
    candleLength: Option[Duration] = None
  )(
    candles: Candle*,
  ): Unit = {
    batchLoader.completeNext(CandleBatch(
      start = start,
      candleLength = candleLength.getOrElse(LiveCandleHistoryLoaderTest.this.candleLength),
      candles = candles,
      nextBatchStart = nextStart,
    ))
  }

  private def expectEmptySegment(start: Instant): Unit = {
    val x = resultFuture.value.get.get
    x shouldEqual candleHistorySegment(start, candleLength)()
  }

  private def expectSegment(candles: Candle*): Unit = {
    val x = resultFuture.value.get.get
    x shouldEqual candleHistorySegment(candles.head, candles.tail: _*)
  }

  private def expectFailure(e: Throwable): Unit = {
    resultFuture.value.get shouldEqual Failure(e)
  }

  private def expectFailure(): Unit = {
    println(resultFuture.value.get)
    resultFuture.value.get should matchPattern { case Failure(_) => () }
  }

  test("if the requested start is in the future it immediately returns an empty segment") {
    loadSegment(sec(105), time = sec(100))
    expectEmptySegment(sec(105))
  }

  test("if the first candle would end in the future it immediately returns an empty segment") {
    loadSegment(sec(100), time = sec(104))
    expectEmptySegment(sec(100))
  }

  test("if the projected first candle end is not in the future it yields a request with the given start") {
    loadSegment(sec(100), time = sec(105))
    batchLoader.verify.apply(sec(100))
  }

  test("returned candles are included in the result up to current time") {
    loadSegment(sec(100), time = sec(114))
    returnBatch(sec(100), None)(
      c5(sec(100), 1),
      c5(sec(105), 1),
      c5(sec(110), 1),
    )
    expectSegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
  }

  test("a candle ending exactly at the current time is included in the result") {
    loadSegment(sec(100), time = sec(105))
    returnBatch(sec(100), None)(
      c5(sec(100), 1),
    )
    expectSegment(
      c5(sec(100), 1),
    )
  }

  test("when there are more batches they are requested and appended") {
    loadSegment(sec(100), time = sec(121))
    batchLoader.verify.apply(sec(100))
    returnBatch(sec(100), Some(sec(110)))(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    batchLoader.verify.apply(sec(110))
    returnBatch(sec(110), None)(
      c5(sec(110), 1),
      c5(sec(115), 1),
    )
    expectSegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
      c5(sec(110), 1),
      c5(sec(115), 1),
    )
  }

  test("it stops requesting segments when the next batch's first candle would end in the future") {
    loadSegment(sec(100), time = sec(114))
    batchLoader.verify.apply(sec(100))
    returnBatch(sec(100), Some(sec(110)))(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    expectSegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    batchLoader.verifyTimes(1).apply(*)
  }

  test("when determining if the next batch start is too late it takes into account the current time") {
    loadSegment(sec(100), time = sec(115))
    returnBatch(sec(100), Some(sec(110)))(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    returnBatch(sec(110), None)(
      c5(sec(110), 1),
    )
    expectSegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
      c5(sec(110), 1),
    )
  }

  test("if the latest batch ends before the current time it fills up the segment with empty candles") {
    loadSegment(sec(100), time = sec(126))
    returnBatch(sec(100), Some(sec(110)))(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    returnBatch(sec(110), None)(
      c5(sec(110), 1),
    )
    expectSegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
      c5(sec(110), 1),
      e5(sec(115)),
      e5(sec(120)),
    )
  }

  test("batches are padded with empty candles if the respective next start is after the latest candle") {
    loadSegment(sec(100), time = sec(126))
    returnBatch(sec(100), Some(sec(120)))(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    batchLoader.verify.apply(sec(120))
    returnBatch(sec(120), None)(
      c5(sec(120), 1),
    )
    expectSegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
      e5(sec(110)),
      e5(sec(115)),
      c5(sec(120), 1),
    )
  }

  test("if one request fails it fails with the same exception") {
    candleLength = secs(5)
    loadSegment(sec(10), time = sec(200))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      c5(sec(15), 1),
    )
    batchLoader.failNext(ex(123))
    expectFailure(ex(123))
  }

  test("it fails when the start of a returned batch does not match the expected start") {
    candleLength = secs(5)
    loadSegment(sec(10), sec(200))
    returnBatch(sec(15), nextStart = Some(sec(25)))(
      c5(sec(15), 1),
    )
    expectFailure()
  }

}
