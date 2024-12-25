package io.liquirium.connect

import io.liquirium.core.{Trade, TradeHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec}
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, matchPattern}

import java.time.Instant
import scala.concurrent.Future
import scala.util.Failure

class BasicExchangeConnectorTest_LoadTrades_WithEnd extends AsyncTestWithScheduler with TestWithMocks {

  private val api =
    new FutureServiceMock[GenericExchangeApi, TradeBatch](_.getTradeBatch(*, *))

  private val tradingPair = pair(1)

  private var resultFuture: Future[TradeHistorySegment] = _

  private def loadSegment(start: Instant, end: Instant): Unit = {
    val connector: BasicExchangeConnector = BasicExchangeConnector.fromExchangeApi(api.instance)
    resultFuture = connector.loadTradeHistory(tradingPair)(start, Some(end))
  }

  private def returnBatch(start: Instant, nextStart: Option[Instant])(trades: Trade*): Unit = {
    api.completeNext(TradeBatch(
      start = start,
      trades = trades,
      nextBatchStart = nextStart,
    ))
  }

  private def expectRequest(start: Instant) = {
    api.verify.getTradeBatch(tradingPair, start)
  }

  test("it immediately yields a request for the batch starting at the given start when no end is given") {
    loadSegment(sec(123), end = sec(456))
    expectRequest(sec(123))
  }

  test("if the returned batch is complete (no next batch start) it returns a segment from the batch trades") {
    loadSegment(sec(10), end = sec(123))
    returnBatch(sec(10), None)(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    )
    resultFuture.value.get.get shouldEqual tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    )
  }

  test("when all trades are before the end, more batches are requested until no more next batch starts are given") {
    loadSegment(sec(10), end = sec(123))
    expectRequest(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(20)))(
      trade(sec(20), "A"),
    )
    expectRequest(sec(20))
    returnBatch(sec(20), nextStart = None)(
      trade(sec(21), "B"),
    )
    api.expectNoFurtherOpenRequests()
  }

  test("when a received trade is at the end, it stops requesting more batches") {
    loadSegment(sec(10), end = sec(123))
    expectRequest(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(20)))(
      trade(sec(20), "A"),
      trade(sec(123), "B"),
    )
    api.expectNoFurtherOpenRequests()
  }

  test("when a received trade is after the end, it stops requesting more batches") {
    loadSegment(sec(10), end = sec(123))
    expectRequest(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(20)))(
      trade(sec(20), "A"),
      trade(sec(124), "B"),
    )
    api.expectNoFurtherOpenRequests()
  }

  test("when the next batch starts at the end, it stops requesting more batches") {
    loadSegment(sec(10), end = sec(123))
    expectRequest(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(123)))(
      trade(sec(20), "A"),
      trade(sec(21), "B"),
    )
    api.expectNoFurtherOpenRequests()
  }

  test("when the next batch starts after the end, it stops requesting more batches") {
    loadSegment(sec(10), end = sec(123))
    expectRequest(sec(10))
    returnBatch(sec(10), nextStart = Some(sec(124)))(
      trade(sec(20), "A"),
      trade(sec(21), "B"),
    )
    api.expectNoFurtherOpenRequests()
  }

  test("when all trades are before the end, all returned batches are combined to a segment") {
    loadSegment(sec(10), end = sec(123))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      trade(sec(12), "A"),
      trade(sec(25), "B"),
    )
    returnBatch(sec(25), nextStart = None)(
      trade(sec(25), "B"),
      trade(sec(30), "C"),
    )
    resultFuture.value.get.get shouldEqual tradeHistorySegment(sec(10))(
      trade(sec(12), "A"),
      trade(sec(25), "B"),
      trade(sec(30), "C"),
    )
  }

  test("trades at and past the end are excluded from the result") {
    loadSegment(sec(10), end = sec(30))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      trade(sec(12), "A"),
      trade(sec(25), "B"),
    )
    returnBatch(sec(25), nextStart = None)(
      trade(sec(25), "B"),
      trade(sec(30), "C"),
      trade(sec(31), "D"),
    )
    resultFuture.value.get.get shouldEqual tradeHistorySegment(sec(10))(
      trade(sec(12), "A"),
      trade(sec(25), "B"),
    )
  }

  test("if one request fails it fails with the same exception") {
    loadSegment(sec(10), end = sec(123))
    returnBatch(sec(10), nextStart = Some(sec(25)))(
      trade(sec(25), "B"),
    )
    api.failNext(ex(123))
    resultFuture.value.get shouldEqual Failure(ex(123))
  }

  test("it fails when the start of a returned batch does not match the expected start") {
    loadSegment(sec(10), end = sec(123))
    returnBatch(sec(11), nextStart = Some(sec(25)))(
      trade(sec(25), "B"),
    )
    resultFuture.value.get should matchPattern { case Failure(_) => () }
  }

}
