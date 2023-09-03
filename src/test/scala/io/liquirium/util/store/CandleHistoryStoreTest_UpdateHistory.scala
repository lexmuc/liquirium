package io.liquirium.util.store

import io.liquirium.core.CandleHistorySegment
import io.liquirium.core.helpers.CandleHelpers.{c10, candleHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}

import scala.concurrent.Future
import scala.util.Failure

class CandleHistoryStoreTest_UpdateHistory extends AsyncTestWithControlledTime with TestWithMocks {

  val baseStoreAddPart = new FutureServiceMock[CandleStore, Unit](
    methodCall = _.add(*),
    extendMock = None,
  )
  val baseStoreDeletePart = new FutureServiceMock[CandleStore, Unit](
    methodCall = _.deleteFrom(*),
    extendMock = Some(baseStoreAddPart.instance),
  )

  private lazy val store = new CandleHistoryStore(baseStoreDeletePart.instance)

  private def update(chs: CandleHistorySegment): Future[Unit] = store.updateHistory(chs)

  test("it adds all the given candles to the store") {
    update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
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
    baseStoreDeletePart.verifyNever.deleteFrom(*)
    baseStoreAddPart.completeNext(())
    baseStoreDeletePart.verify.deleteFrom(sec(120))
  }

  test("when the segment is empty, no candles are added but candles are deleted from the segment start") {
    update(candleHistorySegment(start=sec(100), secs(10))())
    baseStoreAddPart.verifyNever.add(*)
    baseStoreDeletePart.verify.deleteFrom(sec(100))
  }

  test("an error when adding the candles is passed along") {
    val f = update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreAddPart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

  test("an error when deleting the candles is passed along") {
    val f = update(candleHistorySegment(
      c10(sec(100), 1),
      c10(sec(110), 2),
    ))
    baseStoreAddPart.completeNext(())
    baseStoreDeletePart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
