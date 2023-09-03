package io.liquirium.util.store

import io.liquirium.core.CandleHistorySegment
import io.liquirium.core.helpers.CandleHelpers.{c10, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}

import java.time.Instant
import scala.concurrent.Future

class StoreBasedCandleHistoryProviderWithOnDemandUpdateTest extends AsyncTestWithControlledTime with TestWithMocks {

  private val baseStore = mock[CandleHistoryStore]
  private var overlapCandlesCount = 0
  val liveSegmentLoader =
    new FutureServiceMock[Instant => Future[CandleHistorySegment], CandleHistorySegment](_.apply(*))

  private def provider: StoreBasedCandleHistoryProviderWithOnDemandUpdate =
    new StoreBasedCandleHistoryProviderWithOnDemandUpdate(
      baseStore = baseStore,
      overlapCandlesCount = overlapCandlesCount,
      liveSegmentLoader = liveSegmentLoader.instance,
    )

  test("it returns the history from the store if it contains all candles from start to inspection time") {
    baseStore.loadHistory(sec(100), Some(sec(120))) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
        c10(sec(110), 1),
      )
    )
    val f = provider.loadHistory(start = sec(100), inspectionTime = Some(sec(120)))
    f.value.get.get shouldEqual candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 1),
    )
  }

  test("in case the stored candles end before the inspection date, new candles are requested with the given overlap") {
    overlapCandlesCount = 2
    baseStore.loadHistory(sec(100), Some(sec(150))) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
        c10(sec(110), 1),
        c10(sec(120), 1),
      )
    )
    provider.loadHistory(start = sec(100), inspectionTime = Some(sec(150)))
    liveSegmentLoader.verify.apply(sec(110))
  }

  test("in case no inspection time is given, it always performs a live update") {
    overlapCandlesCount = 2
    baseStore.loadHistory(sec(100), inspectionTime = None) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
        c10(sec(110), 1),
        c10(sec(120), 1),
      )
    )
    provider.loadHistory(start = sec(100), inspectionTime = None)
    liveSegmentLoader.verify.apply(sec(110))
  }

  test("the requested live candles segment start is not earlier than the require start") {
    overlapCandlesCount = 2
    baseStore.loadHistory(sec(100), Some(sec(150))) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
      )
    )
    provider.loadHistory(start = sec(100), inspectionTime = Some(sec(150)))
    liveSegmentLoader.verify.apply(sec(100))
  }

  test("the returned segment is extended with the live candles") {
    overlapCandlesCount = 2
    baseStore.loadHistory(sec(100), Some(sec(150))) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
        c10(sec(110), 1),
        c10(sec(120), 1),
      )
    )
    val f = provider.loadHistory(start = sec(100), inspectionTime = Some(sec(150)))
    liveSegmentLoader.completeNext(
      candleHistorySegment(
        c10(sec(110), 1),
        c10(sec(120), 2),
        c10(sec(130), 2),
      )
    )
    f.value.get.get shouldEqual candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 1),
      c10(sec(120), 2),
      c10(sec(130), 2),
    )
  }

  test("the store is updated with the live candles") {
    overlapCandlesCount = 2
    baseStore.loadHistory(sec(100), Some(sec(150))) returns Future.successful(
      candleHistorySegment(
        c10(sec(100), 1),
        c10(sec(110), 1),
        c10(sec(120), 1),
      )
    )
    provider.loadHistory(start = sec(100), inspectionTime = Some(sec(150)))
    val liveSegment = candleHistorySegment(
      c10(sec(110), 1),
      c10(sec(120), 2),
      c10(sec(130), 2),
    )
    liveSegmentLoader.completeNext(liveSegment)
    verify(baseStore).updateHistory(liveSegment)
  }

}
