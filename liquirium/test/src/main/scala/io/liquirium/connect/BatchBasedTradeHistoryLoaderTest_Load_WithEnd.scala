package io.liquirium.connect

import io.liquirium.core.helpers.CoreHelpers.{ex, sec}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{Trade, TradeHistorySegment}

import java.time.Instant
import scala.concurrent.Future
import scala.util.Failure

class BatchBasedTradeHistoryLoaderTest_Load_WithEnd extends AsyncTestWithControlledTime with TestWithMocks {

  private val batchLoader =
    new FutureServiceMock[Instant => Future[TradeBatch], TradeBatch](_.apply(*))

  def load(start: Instant, end: Instant): Future[TradeHistorySegment] = {
    val segmentLoader = new BatchBasedTradeHistoryLoader(
      batchLoader = batchLoader.instance,
    )
    segmentLoader.loadHistory(start, maybeEnd = Some(end))
  }

  private def returnBatch(start: Instant, nextStart: Option[Instant])(trades: Trade*): Unit = {
    batchLoader.completeNext(TradeBatch(
      start = start,
      trades = trades,
      nextBatchStart = nextStart,
    ))
  }

  test("it immediately yields a request for the batch starting at the given start when no end is given") {
    load(sec(123), end = sec(456))
    batchLoader.verify.apply(sec(123))
  }

  test("if the returned batch is complete (no next batch start) it returns a segment from the batch trades") {
    val f = load(sec(10), end = sec(123))
    returnBatch(sec(10), None)(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    )
    f.value.get.get shouldEqual tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    )
  }

  test("when all trades are before the end, more batches are requested until no more next batch starts are given") {
    load(sec(10), end = sec(123))
    batchLoader.verify.apply(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(20)))(
      trade(sec(20), "A"),
    )
    batchLoader.verify.apply(sec(20))
    returnBatch(sec(20), nextStart = None)(
      trade(sec(21), "B"),
    )
    batchLoader.expectNoFurtherOpenRequests()
  }

  test("when a received trade is at the end, it stops requesting more batches") {
    load(sec(10), end = sec(123))
    batchLoader.verify.apply(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(20)))(
      trade(sec(20), "A"),
      trade(sec(123), "B"),
    )
    batchLoader.expectNoFurtherOpenRequests()
  }

  test("when a received trade is after the end, it stops requesting more batches") {
    load(sec(10), end = sec(123))
    batchLoader.verify.apply(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(20)))(
      trade(sec(20), "A"),
      trade(sec(124), "B"),
    )
    batchLoader.expectNoFurtherOpenRequests()
  }

  test("when the next batch starts at the end, it stops requesting more batches") {
    load(sec(10), end = sec(123))
    batchLoader.verify.apply(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(123)))(
      trade(sec(20), "A"),
      trade(sec(21), "B"),
    )
    batchLoader.expectNoFurtherOpenRequests()
  }

  test("when the next batch starts after the end, it stops requesting more batches") {
    load(sec(10), end = sec(123))
    batchLoader.verify.apply(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(124)))(
      trade(sec(20), "A"),
      trade(sec(21), "B"),
    )
    batchLoader.expectNoFurtherOpenRequests()
  }

  test("when all trades are before the end, all returned batches are combined to a segment") {
    val f = load(sec(10), end = sec(123))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      trade(sec(12), "A"),
      trade(sec(25), "B"),
    )
    returnBatch(sec(25), nextStart = None)(
      trade(sec(25), "B"),
      trade(sec(30), "C"),
    )
    f.value.get.get shouldEqual tradeHistorySegment(sec(10))(
      trade(sec(12), "A"),
      trade(sec(25), "B"),
      trade(sec(30), "C"),
    )
  }

  test("trades at and past the end are excluded from the result") {
    val f = load(sec(10), end = sec(30))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      trade(sec(12), "A"),
      trade(sec(25), "B"),
    )
    returnBatch(sec(25), nextStart = None)(
      trade(sec(25), "B"),
      trade(sec(30), "C"),
      trade(sec(31), "D"),
    )
    f.value.get.get shouldEqual tradeHistorySegment(sec(10))(
      trade(sec(12), "A"),
      trade(sec(25), "B"),
    )
  }

  test("if one request fails it fails with the same exception") {
    val f = load(sec(10), end = sec(123))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      trade(sec(25), "B"),
    )
    batchLoader.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

  test("it fails when the start of a returned batch does not match the expected start") {
    val f = load(sec(10), end = sec(123))
    returnBatch(sec(11), nextStart = Some(sec(25)))(
      trade(sec(25), "B"),
    )
    f.value.get should matchPattern { case Failure(_) => () }
  }

}
