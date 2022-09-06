package io.liquirium.connect

import io.liquirium.core.helper.CandleHelpers.{c5, candleHistorySegment, e5}
import io.liquirium.core.helper.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helper.TestWithMocks
import io.liquirium.core.helper.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{Candle, CandleHistorySegment}

import java.time.Instant
import scala.concurrent.Future
import scala.util.Failure

class CandleHistorySegmentLoaderTest extends AsyncTestWithControlledTime with TestWithMocks {

  private val batchLoader =
    new FutureServiceMock[Instant => Future[ForwardCandleBatch], ForwardCandleBatch](_.apply(*))

  private var loaderResolution = secs(5)

  def loadSegment(start: Instant): Future[CandleHistorySegment] = {
    val segmentLoader = new CandleHistorySegmentLoader(
      batchLoader = batchLoader.instance,
    )
    segmentLoader.loadFrom(start)
  }

  private def returnBatch(start: Instant, nextStart: Option[Instant])(candles: Candle*): Unit = {
    batchLoader.completeNext(ForwardCandleBatch(
      start = start,
      resolution = loaderResolution,
      candles = candles,
      nextBatchStart = nextStart,
    ))
  }

  test("it immediately yields a request for the batch starting at the given start") {
    loadSegment(sec(123))
    batchLoader.verify.apply(sec(123))
  }

  test("if the returned batch is complete (no next batch start) it returns a segment from the batch candles") {
    loaderResolution = secs(5)
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
    loaderResolution = secs(5)
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
    loaderResolution = secs(5)
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

  test("if one request fails it fails with the same exception") {
    loaderResolution = secs(5)
    val f = loadSegment(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      c5(sec(15), 1),
    )
    batchLoader.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

  test("it fails when the start of a returned batch does not match the expected start") {
    loaderResolution = secs(5)
    val f = loadSegment(sec(10))
    returnBatch(sec(15), nextStart = Some(sec(25)))(
      c5(sec(15), 1),
    )
    f.value.get should matchPattern { case Failure(_) => () }
  }

}
