package io.liquirium.core

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.util.store.CandleStore

import java.time.Instant
import scala.concurrent.Future

class CandleHistoryCacheTest extends AsyncTestWithControlledTime with TestWithMocks {

  protected val store: CandleStore = mock[CandleStore]
  protected val storeAdd = new FutureServiceMock[CandleStore, Unit](_.add(*), Some(store))
  protected val storeGetStartAndEnd = new FutureServiceMock[CandleStore, Option[(Instant, Instant)]](
    _.getFirstStartAndLastEnd, Some(store),
  )
  protected val storeGet = new FutureServiceMock[CandleStore, Iterable[Candle]](_.get(*, *), Some(store))


  protected def cache: CandleHistoryCache =
    new CandleHistoryCache(
      store = store,
    )

  protected var resultFuture: Future[Either[(Instant, Instant), CandleHistorySegment]] = _

  protected def load(start: Instant, end: Instant): Unit = {
    resultFuture = cache.load(start, end)
  }

  protected def provideStartAndEnd(start: Instant, end: Instant): Unit =
    storeGetStartAndEnd.completeNext(Some((start, end)))

  protected def expectHistoryResult(candles: Candle*): Unit = {
    resultFuture.value.get.get shouldEqual Right(CandleHistorySegment.fromCandles(candles))
  }

  protected def expectUpdateRequestResult(start: Instant, end: Instant): Unit = {
    resultFuture.value.get.get shouldEqual Left((start, end))
  }

  protected def completeStoreGet(candles: Candle*): Unit = {
    storeGet.completeNext(candles)
  }

  protected def expectNoResultYet(): Unit = {
    resultFuture.value shouldEqual None
  }

}
