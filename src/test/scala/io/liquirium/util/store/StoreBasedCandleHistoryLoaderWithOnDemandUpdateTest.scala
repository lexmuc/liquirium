package io.liquirium.util.store

import io.liquirium.core.CandleHistorySegment
import io.liquirium.core.helpers.CandleHelpers.{c10, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}

import java.time.Instant
import scala.concurrent.Future

class StoreBasedCandleHistoryLoaderWithOnDemandUpdateTest extends AsyncTestWithControlledTime with TestWithMocks {

  private val baseStore = mock[CandleHistoryStore]
  private val storeUpdate = new FutureServiceMock[CandleHistoryStore, Unit](_.updateHistory(*), Some(baseStore))
  private var overlapCandlesCount = 0
  val liveSegmentLoader =
    new FutureServiceMock[Instant => Future[CandleHistorySegment], CandleHistorySegment](_.apply(*))

  private def provider: StoreBasedCandleHistoryLoaderWithOnDemandUpdate =
    new StoreBasedCandleHistoryLoaderWithOnDemandUpdate(
      baseStore = baseStore,
      overlapCandlesCount = overlapCandlesCount,
      liveSegmentLoader = liveSegmentLoader.instance,
    )

  test("it returns the history from the store if it contains all candles from start to inspection time") {
    baseStore.load(sec(100), sec(120)) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
        c10(sec(110), 1),
      )
    )
    val f = provider.load(start = sec(100), end = sec(120))
    f.value.get.get shouldEqual candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 1),
    )
  }

  test("in case the stored candles end before the inspection date, new candles are requested with the given overlap") {
    overlapCandlesCount = 2
    baseStore.load(sec(100), sec(150)) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
        c10(sec(110), 1),
        c10(sec(120), 1),
      )
    )
    provider.load(start = sec(100), end = sec(150))
    liveSegmentLoader.verify.apply(sec(110))
  }

  test("the requested live candles segment start is not earlier than the required start") {
    overlapCandlesCount = 2
    baseStore.load(sec(100), sec(150)) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
      )
    )
    provider.load(start = sec(100), end = sec(150))
    liveSegmentLoader.verify.apply(sec(100))
  }

  test("the store is updated with the live candles") {
    overlapCandlesCount = 2
    baseStore.load(sec(100), sec(150)) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
        c10(sec(110), 1),
        c10(sec(120), 1),
      )
    )
    provider.load(start = sec(100), end = sec(150))
    val liveSegment = candleHistorySegment(
      c10(sec(110), 1),
      c10(sec(120), 2),
      c10(sec(130), 2),
    )
    liveSegmentLoader.completeNext(liveSegment)
    verify(baseStore).updateHistory(liveSegment)
  }

  test("it does not return before the store update is complete") {
    overlapCandlesCount = 0
    baseStore.load(sec(100), sec(150)) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
      )
    )
    val f = provider.load(start = sec(100), end = sec(150))
    val liveSegment = candleHistorySegment(
      c10(sec(110), 1),
    )
    liveSegmentLoader.completeNext(liveSegment)
    f.value shouldEqual None
    storeUpdate.completeNext(())
    f.value.isDefined shouldBe true
  }

  test("the returned segment is extended with the live candles") {
    overlapCandlesCount = 2
    baseStore.load(sec(100), sec(140)) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
        c10(sec(110), 1),
        c10(sec(120), 1),
      )
    )
    val f = provider.load(start = sec(100), end = sec(140))
    liveSegmentLoader.completeNext(
      candleHistorySegment(
        c10(sec(110), 1),
        c10(sec(120), 2),
        c10(sec(130), 2),
      )
    )
    storeUpdate.completeNext(())
    f.value.get.get shouldEqual candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 1),
      c10(sec(120), 2),
      c10(sec(130), 2),
    )
  }

  test("the live candles are truncated if they exceed the inspection time") {
    overlapCandlesCount = 0
    baseStore.load(sec(100), sec(120)) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
      )
    )
    val f = provider.load(start = sec(100), end = sec(120))
    liveSegmentLoader.completeNext(
      candleHistorySegment(
        c10(sec(110), 1),
        c10(sec(120), 2),
        c10(sec(130), 2),
      )
    )
    storeUpdate.completeNext(())
    f.value.get.get shouldEqual candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 1),
    )
  }

}
