package io.liquirium.connect

import io.liquirium.core.helpers.CandleHelpers.{c5, candleHistorySegment, e5}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.MarketHelpers.pair
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import io.liquirium.core.{Candle, CandleHistorySegment}
import org.scalatest.matchers.should.Matchers._

import java.time.{Duration, Instant}
import scala.concurrent.Future
import scala.util.Failure

class BasicExchangeConnectorTest_LoadCandles extends AsyncTestWithScheduler with TestWithMocks {

  private val api = new FutureServiceMock[GenericExchangeApi, CandleBatch](_.getCandleBatch(*, *, *))

  private val candleLength = secs(5)
  private val tradingPair = pair(1)

  private var resultFuture: Future[CandleHistorySegment] = _

  private def loadSegment(start: Instant, maybeEnd: Option[Instant]): Unit = {
    val connector: BasicExchangeConnector = BasicExchangeConnector.fromExchangeApi(api.instance)
    resultFuture = connector.loadCandleHistory(tradingPair, candleLength, start, maybeEnd)
  }

  private def returnBatch(
    start: Instant,
    nextStart: Option[Instant],
    candleLength: Option[Duration] = None
  )(
    candles: Candle*,
  ): Unit = {
    api.completeNext(CandleBatch(
      start = start,
      candleLength = candleLength.getOrElse(BasicExchangeConnectorTest_LoadCandles.this.candleLength),
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
    resultFuture.value.get should matchPattern { case Failure(_) => () }
  }

  private def expectRequest(start: Instant) = {
    api.verify.getCandleBatch(tradingPair, candleLength, start)
  }

  test("it immediately requests the candles from the given start") {
    loadSegment(sec(105), maybeEnd = None)
    expectRequest(sec(105))
  }

  test("it returns the empty segment if an empty batch (no next batch) is returned") {
    loadSegment(sec(105), maybeEnd = None)
    returnBatch(sec(105), None)()
    expectEmptySegment(sec(105))
  }

  test("if no end is given and the returned batch is complete (no next batch) it returns the candles") {
    loadSegment(sec(105), maybeEnd = None)
    returnBatch(sec(105), None)(
      c5(sec(105), 1),
      c5(sec(110), 1),
      c5(sec(115), 1),
    )
    expectSegment(
      c5(sec(105), 1),
      c5(sec(110), 1),
      c5(sec(115), 1),
    )
  }

  test("if an end is given, it truncates the result at the end") {
    loadSegment(sec(105), maybeEnd = Some(sec(115)))
    returnBatch(sec(105), None)(
      c5(sec(105), 1),
      c5(sec(110), 1),
      c5(sec(115), 1),
    )
    expectSegment(
      c5(sec(105), 1),
      c5(sec(110), 1),
    )
  }

  test("if the end does not align with candle starts the result is truncated so that no candle extends the end") {
    loadSegment(sec(105), maybeEnd = Some(sec(114)))
    returnBatch(sec(105), None)(
      c5(sec(105), 1),
      c5(sec(110), 1),
    )
    expectSegment(
      c5(sec(105), 1),
    )
  }

  test("if no end is given it requests more candles until there is no next batch") {
    loadSegment(sec(100), maybeEnd = None)
    expectRequest(sec(100))
    returnBatch(sec(100), Some(sec(110)))(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    expectRequest(sec(110))
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

  test("if an end is given it requests more candles until the end is reached") {
    loadSegment(sec(100), maybeEnd = Some(sec(115)))
    expectRequest(sec(100))
    returnBatch(sec(100), Some(sec(110)))(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    expectRequest(sec(110))
    returnBatch(sec(110), Some(sec(120)))(
      c5(sec(110), 1),
      c5(sec(115), 1),
    )
    expectSegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
      c5(sec(110), 1),
    )
    api.expectNoFurtherOpenRequests()
  }

  test("it stops requesting when the end is exactly at the end of the current batch") {
    loadSegment(sec(100), maybeEnd = Some(sec(110)))
    expectRequest(sec(100))
    returnBatch(sec(100), Some(sec(110)))(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    expectSegment(
      c5(sec(100), 1),
      c5(sec(105), 1),
    )
    api.expectNoFurtherOpenRequests()
  }

  test("there may be gaps in the batches and they are filled with empty candles") {
    loadSegment(sec(100), maybeEnd = None)
    expectRequest(sec(100))
    returnBatch(sec(100), Some(sec(115)))(
      c5(sec(105), 1),
    )
    expectRequest(sec(115))
    returnBatch(sec(115), None)(
      c5(sec(120), 1),
    )
    expectSegment(
      e5(sec(100)),
      c5(sec(105), 1),
      e5(sec(110)),
      e5(sec(115)),
      c5(sec(120), 1),
    )
  }

    test("if the latest batch ends before the end time it fills up the segment with empty candles") {
      loadSegment(sec(100), maybeEnd = Some(sec(126)))
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

    test("if one request fails it fails with the same exception") {
      loadSegment(sec(10), maybeEnd = None)
      returnBatch(sec(10), nextStart = Some(sec(25)))(
        c5(sec(15), 1),
      )
      api.failNext(ex(123))
      expectFailure(ex(123))
    }

    test("it fails when the start of a returned batch does not match the expected start") {
      loadSegment(sec(10), maybeEnd = None)
      returnBatch(sec(15), nextStart = Some(sec(25)))(
        c5(sec(15), 1),
      )
      expectFailure()
    }

}
