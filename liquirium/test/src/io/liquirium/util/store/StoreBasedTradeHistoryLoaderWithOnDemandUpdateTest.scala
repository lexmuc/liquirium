package io.liquirium.util.store

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import io.liquirium.core.{Trade, TradeHistorySegment}
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.{Duration, Instant}
import scala.concurrent.Future

class StoreBasedTradeHistoryLoaderWithOnDemandUpdateTest extends AsyncTestWithScheduler with TestWithMocks {

  private val baseStore = mock(classOf[TradeHistoryStore])
  private val storeUpdate = new FutureServiceMock[TradeHistoryStore, Unit](_.updateHistory(*), Some(baseStore))
  private var overlapDuration = Duration.ofSeconds(0)
  val liveSegmentLoader =
    new FutureServiceMock[Instant => Future[TradeHistorySegment], TradeHistorySegment](_.apply(*))

  private def provider: StoreBasedTradeHistoryLoaderWithOnDemandUpdate =
    new StoreBasedTradeHistoryLoaderWithOnDemandUpdate(
      baseStore = baseStore,
      liveSegmentLoader = liveSegmentLoader.instance,
      overlapDuration = overlapDuration,
    )

  private def fakeStoreTrades(start: Instant, inspectionTime: Option[Instant])(tt: Trade*): Unit =
    baseStore.loadHistory(start, inspectionTime) returns Future.successful {
      tradeHistorySegment(start)(tt: _*)
    }

  test("it requests the live segment from the last known trade before the inspection time minus overlap duration") {
    overlapDuration = Duration.ofSeconds(4)
    fakeStoreTrades(sec(100), Some(sec(120)))(
      trade(sec(110), "A"),
      trade(sec(112), "B"),
    )
    provider.loadHistory(start = sec(100), maybeEnd = Some(sec(120)))
    liveSegmentLoader.verify.apply(sec(108))
  }

  test("the live segment request start is at least the required start, not earlier") {
    overlapDuration = Duration.ofSeconds(15)
    fakeStoreTrades(sec(100), Some(sec(120)))(
      trade(sec(110), "A"),
      trade(sec(112), "B"),
    )
    provider.loadHistory(start = sec(100), maybeEnd = Some(sec(120)))
    liveSegmentLoader.verify.apply(sec(100))
  }

  test("the store is updated with all new trades even if the returned segment is truncated") {
    overlapDuration = Duration.ofSeconds(5)
    fakeStoreTrades(sec(100), Some(sec(120)))(
      trade(sec(110), "A"),
    )
    provider.loadHistory(start = sec(100), maybeEnd = Some(sec(120)))
    val liveSegment =
      tradeHistorySegment(sec(105))(
        trade(sec(110), "A"),
        trade(sec(119), "B"),
        trade(sec(122), "C"),
      )
    liveSegmentLoader.completeNext(liveSegment)
    storeUpdate.verify.updateHistory(liveSegment)
  }

  test("it does not return before the store update is complete") {
    overlapDuration = Duration.ofSeconds(5)
    fakeStoreTrades(sec(100), Some(sec(120)))(
      trade(sec(110), "A"),
    )
    val f = provider.loadHistory(start = sec(100), maybeEnd = Some(sec(120)))
    val liveSegment =
      tradeHistorySegment(sec(105))(
        trade(sec(110), "A"),
        trade(sec(119), "B"),
        trade(sec(122), "C"),
      )
    liveSegmentLoader.completeNext(liveSegment)
    f.value shouldEqual None
    storeUpdate.completeNext(())
    f.value.isDefined shouldBe true
  }

  test("it returns the stored segment extended with the live segment") {
    overlapDuration = Duration.ofSeconds(3)
    fakeStoreTrades(sec(100), Some(sec(120)))(
      trade(sec(110), "A"),
      trade(sec(112), "B"),
      trade(sec(114), "C"),
    )
    val f = provider.loadHistory(start = sec(100), maybeEnd = Some(sec(120)))
    liveSegmentLoader.completeNext(
      tradeHistorySegment(sec(111))(
        trade(sec(112), "B"),
        trade(sec(113), "C2"),
        trade(sec(119), "D"),
      )
    )
    storeUpdate.completeNext(())
    f.value.get.get shouldEqual tradeHistorySegment(sec(100))(
      trade(sec(110), "A"),
      trade(sec(112), "B"),
      trade(sec(113), "C2"),
      trade(sec(119), "D"),
    )
  }

  test("the returned segment is truncated at the inspection time if given") {
    overlapDuration = Duration.ofSeconds(5)
    fakeStoreTrades(sec(100), Some(sec(120)))(
      trade(sec(110), "A"),
    )
    val f = provider.loadHistory(start = sec(100), maybeEnd = Some(sec(120)))
    liveSegmentLoader.completeNext(
      tradeHistorySegment(sec(105))(
        trade(sec(110), "A"),
        trade(sec(119), "B"),
        trade(sec(120), "C"),
      )
    )
    storeUpdate.completeNext(())
    f.value.get.get shouldEqual tradeHistorySegment(sec(100))(
      trade(sec(110), "A"),
      trade(sec(119), "B"),
    )
  }

  test("the returned segment is not truncated when no inspection time is given") {
    overlapDuration = Duration.ofSeconds(5)
    fakeStoreTrades(sec(100), None)(
      trade(sec(110), "A"),
    )
    val f = provider.loadHistory(start = sec(100), maybeEnd = None)
    liveSegmentLoader.completeNext(
      tradeHistorySegment(sec(105))(
        trade(sec(110), "A"),
        trade(sec(119), "B"),
        trade(sec(120), "C"),
      )
    )
    storeUpdate.completeNext(())
    f.value.get.get shouldEqual tradeHistorySegment(sec(100))(
      trade(sec(110), "A"),
      trade(sec(119), "B"),
      trade(sec(120), "C"),
    )
  }

}
