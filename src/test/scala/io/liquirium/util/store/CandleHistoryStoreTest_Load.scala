package io.liquirium.util.store

import io.liquirium.core.helpers.CandleHelpers.{c10, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{Candle, CandleHistorySegment}

import java.time.Instant
import scala.concurrent.Future
import scala.util.{Failure, Success}

class CandleHistoryStoreTest_Load extends AsyncTestWithControlledTime with TestWithMocks {

  val baseLoader: CandleStore = mock[CandleStore]
  baseLoader.candleLength returns secs(10)

  val baseStoreLoaderPart = new FutureServiceMock[CandleStore, Iterable[Candle]](
    methodCall = _.get(*, *),
    extendMock = Some(baseLoader),
  )

  private lazy val store = new CandleHistoryStore(baseStoreLoaderPart.instance)

  private def load(start: Instant, time: Instant): Future[CandleHistorySegment] =
    store.load(start, time)

  test("for a given start and time it requests all candles from the base store from the start up to the time") {
    load(sec(100), sec(200))
    baseStoreLoaderPart.verify.get(from = Some(sec(100)), until = Some(sec(200)))
  }

  test("the candles are returned as a candle history segment which may be shorter than requested") {
    val f = load(sec(100), sec(200))
    baseStoreLoaderPart.completeNext(
      Seq(c10(sec(100), 1), c10(sec(110), 2))
    )
    f.value.get shouldEqual Success(candleHistorySegment(c10(sec(100), 1), c10(sec(110), 2)))
  }

  test("when there are no candles, the empty segment is returned") {
    val f = load(sec(100), sec(200))
    baseStoreLoaderPart.completeNext(Seq())
    f.value.get shouldEqual Success(candleHistorySegment(sec(100), secs(10))())
  }

  test("when the first candle ist not the segment start, the empty segment is returned") {
    val f = load(sec(100), sec(200))
    baseStoreLoaderPart.completeNext(Seq(c10(sec(110), 1)))
    f.value.get shouldEqual Success(candleHistorySegment(sec(100), secs(10))())
  }

  test("an error when getting the candles is passed along") {
    val f = load(sec(100), sec(200))
    baseStoreLoaderPart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}