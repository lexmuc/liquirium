package io.liquirium.connect

import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment, e5}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{Candle, CandleHistorySegment}

import java.time.Instant
import scala.concurrent.Future
import scala.util.Failure

class CandleHistorySegmentLoaderTest extends AsyncTestWithControlledTime with TestWithMocks {

  private val batchLoader =
    new FutureServiceMock[Instant => Future[CandleBatch], CandleBatch](_.apply(*))

  private var dropLatest = false

  private var loaderCandleLength = secs(5)

  def loadSegment(start: Instant): Future[CandleHistorySegment] = {
    val segmentLoader = new CandleHistorySegmentLoader(
      batchLoader = batchLoader.instance,
      dropLatest = dropLatest,
    )
    segmentLoader.loadFrom(start)
  }

  private def returnBatch(start: Instant, nextStart: Option[Instant])(candles: Candle*): Unit = {
    batchLoader.completeNext(CandleBatch(
      start = start,
      candleLength = loaderCandleLength,
      candles = candles,
      nextBatchStart = nextStart,
    ))
  }

  test("it immediately yields a request for the batch starting at the given start") {
    loadSegment(sec(123))
    batchLoader.verify.apply(sec(123))
  }

  test("if the returned batch is complete (no next batch start) it returns a segment from the batch candles") {
    loaderCandleLength = secs(5)
    val f = loadSegment(sec(10))
    returnBatch(sec(10), None)(
      c5(sec(15), 1),
      c5(sec(25), 1),
    )
    f.value.get.get shouldEqual candleHistorySegment(
      e5(sec(10)),
      c5(sec(15), 1),
      e5(sec(20)),
      c5(sec(25), 1),
    )
  }

  test("more batches are requested until no more next batch starts are given") {
    loaderCandleLength = secs(5)
    loadSegment(sec(10))
    batchLoader.verify.apply(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(20)))(
      c5(sec(15), 1),
    )
    batchLoader.verify.apply(sec(20))
    returnBatch(sec(20), nextStart = None)(
      c5(sec(20), 1),
    )
    batchLoader.expectNoFurtherOpenRequests()
  }

  test("all returned batches are combined to a segment") {
    loaderCandleLength = secs(5)
    val f = loadSegment(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      c5(sec(15), 1),
    )
    returnBatch(sec(25), nextStart = None)(
      c5(sec(25), 1),
    )
    f.value.get.get shouldEqual candleHistorySegment(
      e5(sec(10)),
      c5(sec(15), 1),
      e5(sec(20)),
      c5(sec(25), 1),
    )
  }

  test("it can be configured to drop the latest candle") {
    dropLatest = true
    loaderCandleLength = secs(5)
    val f = loadSegment(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(20)))(
      c5(sec(10), 1),
      c5(sec(15), 1),
    )
    returnBatch(sec(20), nextStart = None)(
      c5(sec(20), 1),
      c5(sec(25), 1),
    )
    f.value.get.get shouldEqual candleHistorySegment(
      c5(sec(10), 1),
      c5(sec(15), 1),
      c5(sec(20), 1),
    )
  }

  test("when the latest candle is to be dropped and no candles are found, no candles are returned") {
    dropLatest = true
    loaderCandleLength = secs(5)
    val f = loadSegment(sec(10))
    returnBatch(sec(10), nextStart = None)(
    )
    f.value.get.get shouldEqual candleHistorySegment(sec(10), secs(5))()
  }

  test("if one request fails it fails with the same exception") {
    loaderCandleLength = secs(5)
    val f = loadSegment(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      c5(sec(15), 1),
    )
    batchLoader.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

  test("it fails when the start of a returned batch does not match the expected start") {
    loaderCandleLength = secs(5)
    val f = loadSegment(sec(10))
    returnBatch(sec(15), nextStart = Some(sec(25)))(
      c5(sec(15), 1),
    )
    f.value.get should matchPattern { case Failure(_) => () }
  }

}
