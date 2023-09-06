package io.liquirium.util.store

import io.liquirium.core.{Candle, CandleHistorySegment}
import io.liquirium.core.helpers.CandleHelpers.{c10, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}

import scala.concurrent.Future
import scala.util.Failure

class CandleHistoryStoreTest_UpdateHistory extends AsyncTestWithControlledTime with TestWithMocks {

  val storeMock: CandleStore = mock[CandleStore]

  val baseStoreGetPart = new FutureServiceMock[CandleStore, Iterable[Candle]](
    methodCall = _.get(*, *),
    extendMock = Some(storeMock)
  )
  val baseStoreAddPart = new FutureServiceMock[CandleStore, Unit](
    methodCall = _.add(*),
    extendMock = Some(storeMock)
  )
  val baseStoreDeleteFromPart = new FutureServiceMock[CandleStore, Unit](
    methodCall = _.deleteFrom(*),
    extendMock = Some(storeMock),
  )

  val baseStoreDeleteBeforePart = new FutureServiceMock[CandleStore, Unit](
    methodCall = _.deleteBefore(*),
    extendMock = Some(storeMock),
  )

  private lazy val store = new CandleHistoryStore(baseStoreDeleteFromPart.instance)

  private def update(chs: CandleHistorySegment): Future[Unit] = store.updateHistory(chs)

  test("if there is no candle before the segment, everything before it is deleted in order to avoid gaps") {
    update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreGetPart.verify.get(from = Some(sec(90)), until = Some(sec(100)))
    baseStoreGetPart.completeNext(Seq())
    baseStoreDeleteBeforePart.verify.deleteBefore(sec(100))
  }

  test("if the new segment is directly preceded by a candle, the candles before are not deleted") {
    update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreGetPart.verify.get(from = Some(sec(90)), until = Some(sec(100)))
    baseStoreGetPart.completeNext(Seq(
      c10(sec(90), 1),
    ))
    baseStoreDeleteBeforePart.verifyNever.deleteBefore(*)
  }

  test("it adds all the given candles to the store") {
    update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreGetPart.completeNext(Seq(c10(sec(90), 1)))
    baseStoreAddPart.verify.add(Seq(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
  }

  test("candles after the end of the segment are deleted after the candles have been added") {
    update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreGetPart.completeNext(Seq(c10(sec(90), 1)))
    baseStoreDeleteFromPart.verifyNever.deleteFrom(*)
    baseStoreAddPart.completeNext(())
    baseStoreDeleteFromPart.verify.deleteFrom(sec(120))
  }

  test("when the segment is empty, no candles are added but candles are deleted from the segment start") {
    update(candleHistorySegment(start=sec(100), secs(10))())
    baseStoreGetPart.completeNext(Seq(c10(sec(90), 1)))
    baseStoreAddPart.verifyNever.add(*)
    baseStoreDeleteFromPart.verify.deleteFrom(sec(100))
  }

  test("when the new segment is empty, earlier candles are deleted, too, when there is no preceding candle") {
    update(candleHistorySegment(start = sec(100), secs(10))())
    baseStoreGetPart.completeNext(Seq())
    baseStoreDeleteBeforePart.verify.deleteBefore(sec(100))
    baseStoreDeleteBeforePart.completeNext(())
    baseStoreAddPart.verifyNever.add(*)
    baseStoreDeleteFromPart.verify.deleteFrom(sec(100))
  }

  test("an error when getting the preceding candle is passed along") {
    val f = update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreGetPart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

  test("an error when deleting earlier candles is passed along") {
    val f = update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreGetPart.completeNext(Seq())
    baseStoreDeleteBeforePart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

  test("an error when adding the candles is passed along") {
    val f = update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreGetPart.completeNext(Seq(c10(sec(90), 1)))
    baseStoreAddPart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

  test("an error when deleting the candles is passed along") {
    val f = update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreGetPart.completeNext(Seq(c10(sec(90), 1)))
    baseStoreAddPart.completeNext(())
    baseStoreDeleteFromPart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
