package io.liquirium.core

import io.liquirium.connect.TradeBatch
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeBatch, tradeHistorySegment}
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import io.liquirium.util.store.{TradeSegmentStartStore, TradeStore}
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.{a, convertToAnyShouldWrapper}

import java.time.Instant
import scala.util.Success

class TradeHistoryCacheTest extends AsyncTestWithScheduler with TestWithMocks {

  protected val tradeStore: TradeStore = mock(classOf[TradeStore])
  protected val tradeStoreAdd = new FutureServiceMock[TradeStore, Unit](_.add(*), Some(tradeStore))
  protected val tradeStoreGet = new FutureServiceMock[TradeStore, TradeBatch](_.get(*, *), Some(tradeStore))
  protected val tradeStoreDelete = new FutureServiceMock[TradeStore, Unit](_.deleteFrom(*), Some(tradeStore))

  protected val startStore: TradeSegmentStartStore = mock(classOf[TradeSegmentStartStore])
  protected val startStoreSaveStart: FutureServiceMock[TradeSegmentStartStore, Unit] = {
    new FutureServiceMock[TradeSegmentStartStore, Unit](_.saveStart(*), Some(startStore))
  }
  protected val startStoreReadStart: FutureServiceMock[TradeSegmentStartStore, Option[Instant]] = {
    new FutureServiceMock[TradeSegmentStartStore, Option[Instant]](_.readStart, Some(startStore))
  }

  protected val cache: TradeHistoryCache =
    new TradeHistoryCache(
      tradeStore = tradeStore,
      startStore = startStore,
    )

  test("when saving a segment, the start is stored in the start store") {
    val segment = tradeHistorySegment(sec(123))(
      trade(sec(150), "A"),
    )
    cache.save(segment)
    startStoreSaveStart.verify.saveStart(sec(123))
  }

  test("when saving a segment, the trade store is emptied and trades are saved") {
    val segment = tradeHistorySegment(sec(123))(
      trade(sec(150), "A"),
      trade(sec(170), "B"),
    )
    cache.save(segment)
    startStoreSaveStart.completeNext(())
    tradeStoreDelete.verify.deleteFrom(sec(0))
    tradeStoreDelete.completeNext(())
    tradeStoreAdd.verify.add(segment)
  }

  test("when saving a non-empty segment it only returns when the trades have been added") {
    val segment = tradeHistorySegment(sec(123))(
      trade(sec(150), "A"),
      trade(sec(170), "B"),
    )
    val f = cache.save(segment)
    startStoreSaveStart.completeNext(())
    tradeStoreDelete.completeNext(())
    f.value.isDefined shouldBe false
    tradeStoreAdd.completeNext(())
    f.value.isDefined shouldBe true
  }

  test("when saving an empty segment it returns already after clearing the trade store") {
    val segment = tradeHistorySegment(sec(123))()
    val f = cache.save(segment)
    startStoreSaveStart.completeNext(())
    f.value.isDefined shouldBe false
    tradeStoreDelete.completeNext(())
    f.value.isDefined shouldBe true
  }

  // read segment (Option)
  test("when reading a segment and there is no start, the result is None") {
    val f = cache.read()
    startStoreReadStart.verify.readStart
    startStoreReadStart.completeNext(None)
    f.value.get.get shouldEqual None
  }

  test("when there is a start, the trades are read from the store and a segment is returned") {
    val f = cache.read()
    startStoreReadStart.completeNext(Some(sec(123)))
    tradeStoreGet.verify.get(Some(sec(123)), None)
    tradeStoreGet.completeNext(tradeBatch(sec(123))(
      trade(sec(150), "A"),
      trade(sec(170), "B"),
    ))
    f.value.get.get shouldEqual Some(
      tradeHistorySegment(sec(123))(
        trade(sec(150), "A"),
        trade(sec(170), "B"),
      )
    )
  }

  test("an empty segment is properly returned") {
    val f = cache.read()
    startStoreReadStart.completeNext(Some(sec(123)))
    tradeStoreGet.verify.get(Some(sec(123)), None)
    tradeStoreGet.completeNext(tradeBatch(sec(123))())
    f.value.get.get shouldEqual Some(
      tradeHistorySegment(sec(123))()
    )
  }

  test("it throws an exception when the start is empty and it is tried to extend the stored segment") {
    val f = cache.extendWith(
      tradeHistorySegment(sec(123))()
    )
    startStoreReadStart.verify.readStart
    startStoreReadStart.completeNext(None)
    f.value.get.failed.get shouldBe a [RuntimeException]
  }

  test("when the extension start is the segment start, all trades are replaced") {
    val f = cache.extendWith(
      tradeHistorySegment(sec(123))(
        trade(sec(150), "A"),
        trade(sec(170), "B"),
      )
    )
    startStoreReadStart.completeNext(Some(sec(123)))
    tradeStoreDelete.verify.deleteFrom(sec(123))
    tradeStoreDelete.completeNext(())
    tradeStoreAdd.verify.add(Seq(
      trade(sec(150), "A"),
      trade(sec(170), "B"),
    ))
    tradeStoreAdd.completeNext(())
    f.value.get shouldEqual Success(())
  }

  test("it reads the trades from the extension start when the extension start is after the segment start") {
    cache.extendWith(
      tradeHistorySegment(sec(123))(
        trade(sec(150), "A"),
        trade(sec(170), "B"),
      )
    )
    startStoreReadStart.completeNext(Some(sec(100)))
    tradeStoreGet.verify.get(Some(sec(123)), None)
  }

  test("when trades are found at the extension start it replaces trades from there on") {
    val f = cache.extendWith(
      tradeHistorySegment(sec(123))(
        trade(sec(150), "A"),
        trade(sec(170), "B"),
      )
    )
    startStoreReadStart.completeNext(Some(sec(100)))
    tradeStoreGet.verify.get(Some(sec(123)), None)
    tradeStoreGet.completeNext(tradeBatch(sec(123))(
      trade(sec(123), "X"),
      trade(sec(151), "X"),
    ))
    tradeStoreDelete.verify.deleteFrom(sec(123))
    tradeStoreDelete.completeNext(())
    tradeStoreAdd.verify.add(Seq(
      trade(sec(150), "A"),
      trade(sec(170), "B"),
    ))
    tradeStoreAdd.completeNext(())
    f.value.get shouldEqual Success(())
  }

  test("an exception is thrown when no trades are found at a later extension start") {
    val f = cache.extendWith(
      tradeHistorySegment(sec(123))(
        trade(sec(150), "A"),
        trade(sec(170), "B"),
      )
    )
    startStoreReadStart.completeNext(Some(sec(100)))
    tradeStoreGet.verify.get(Some(sec(123)), None)
    tradeStoreGet.completeNext(tradeBatch(sec(123))())
    f.value.get.failed.get shouldBe a [RuntimeException]
  }

  test("it throws an exception when the extension start is before the start") {
    val f = cache.extendWith(
      tradeHistorySegment(sec(99))(
        trade(sec(150), "A"),
        trade(sec(170), "B"),
      )
    )
    startStoreReadStart.completeNext(Some(sec(100)))
    f.value.get.failed.get shouldBe a[RuntimeException]
  }

  test("extending a segment only returns after the trades have been added") {
    val f = cache.extendWith(
      tradeHistorySegment(sec(123))(
        trade(sec(150), "A"),
        trade(sec(170), "B"),
      )
    )
    startStoreReadStart.completeNext(Some(sec(100)))
    tradeStoreGet.completeNext(tradeBatch(sec(123))(
      trade(sec(123), "X"),
      trade(sec(151), "X"),
    ))
    tradeStoreDelete.completeNext(())
    f.value.isDefined shouldBe false
    tradeStoreAdd.completeNext(())
    f.value.get shouldEqual Success(())
  }

}
