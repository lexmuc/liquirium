package io.liquirium.core

import io.liquirium.core.helpers.CandleHelpers.{c10, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}

import java.time.{Duration, Instant}
import scala.concurrent.Future
import scala.util.Failure

class CachingCandleHistoryLoaderTest extends AsyncTestWithControlledTime with TestWithMocks {

  protected var candleLength: Duration = Duration.ofSeconds(1)

  protected val cache: CandleHistoryCache = mock[CandleHistoryCache]
  protected val cacheLoad = new FutureServiceMock[
    CandleHistoryCache,
    Either[(Instant, Instant), CandleHistorySegment],
  ](_.load(*, *), Some(cache))
  protected val cacheExtend = new FutureServiceMock[CandleHistoryCache, Unit](_.extendWith(*), Some(cache))

  protected val baseLoader = new FutureServiceMock[CandleHistoryLoader, CandleHistorySegment](_.load(*, *))

  protected def loader: CachingCandleHistoryLoader =
    new CachingCandleHistoryLoader(
      candleLength = candleLength,
      baseLoader = baseLoader.instance,
      cache = cache,
    )

  protected var resultFuture: Future[CandleHistorySegment] = _

  protected def load(start: Instant, end: Instant): Unit = {
    resultFuture = loader.load(start, end)
  }

  protected def expectBaseLoaderRequest(start: Instant, end: Instant): Unit = {
    baseLoader.verify.load(start, end)
  }

  protected def completeBaseLoaderRequest(candles: Candle*): Unit = {
    baseLoader.completeNext(CandleHistorySegment.fromCandles(candles))
  }

  protected def completeBaseLoaderRequestWithEmptySegment(start: Instant): Unit = {
    baseLoader.completeNext(CandleHistorySegment.empty(start, candleLength))
  }

  protected def expectResult(candles: Candle*): Unit = {
    resultFuture.value.get.get shouldEqual CandleHistorySegment.fromCandles(candles)
  }

  protected def expectEmptyResult(start: Instant): Unit = {
    resultFuture.value.get.get shouldEqual CandleHistorySegment.empty(start, candleLength)
  }

  protected def expectNoResultYet(): Unit = {
    resultFuture.value shouldEqual None
  }

  test("it tries to obtain the history from the cache and returns it if successful") {
    load(sec(100), sec(120))
    cacheLoad.verify.load(sec(100), sec(120))
    val resultSegment = candleHistorySegment(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
    cacheLoad.completeNext(Right(resultSegment))
    resultFuture.value.get.get shouldEqual resultSegment
  }

  test("if the cache responds with a request for candles, they are loaded, the cache updated and queried again") {
    load(sec(100), sec(120))
    cacheLoad.completeNext(Left(sec(110), sec(120)))
    expectBaseLoaderRequest(sec(110), sec(120))
    completeBaseLoaderRequest(
      c10(sec(110), 234),
    )
    cacheExtend.verify.extendWith(
      candleHistorySegment(
        c10(sec(110), 234),
      ),
    )
    cacheExtend.completeNext(())
    cacheLoad.verifyTimes(2).load(sec(100), sec(120))
    val resultSegment = candleHistorySegment(
      c10(sec(100), 123),
      c10(sec(110), 123),
    )
    cacheLoad.completeNext(Right(resultSegment))
    resultFuture.value.get.get shouldEqual resultSegment
  }

  test("if the cache responds with another request for candles after the update it throws a RuntimeException") {
    load(sec(100), sec(120))
    cacheLoad.completeNext(Left(sec(110), sec(120)))
    completeBaseLoaderRequest(
      c10(sec(110), 234),
    )
    cacheExtend.completeNext(())
    cacheLoad.completeNext(Left(sec(110), sec(120)))
    resultFuture.value.get shouldBe a[Failure[_]]
    resultFuture.value.get.failed.get shouldBe a[RuntimeException]
  }

  test("after an incomplete update at the end of the period, the cache is only queried again with an earlier end") {
    // this will obviously happen when requesting candles up to the present and the exchange cannot provide them yet
    load(sec(100), sec(140))
    cacheLoad.completeNext(Left(sec(110), sec(140)))
    expectBaseLoaderRequest(sec(110), sec(140))
    completeBaseLoaderRequest(
      c10(sec(110), 234),
      c10(sec(120), 234),
    )
    cacheExtend.verify.extendWith(
      candleHistorySegment(
        c10(sec(110), 234),
        c10(sec(120), 234),
      ),
    )
    cacheExtend.completeNext(())
    cacheLoad.verify.load(sec(100), sec(130))
    val resultSegment = candleHistorySegment(
      c10(sec(100), 123),
      c10(sec(110), 234),
      c10(sec(120), 234),
    )
    cacheLoad.completeNext(Right(resultSegment))
    resultFuture.value.get.get shouldEqual resultSegment
  }

}
