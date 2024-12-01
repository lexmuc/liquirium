package io.liquirium.util.store

import io.liquirium.core.TradeHistorySegment
import io.liquirium.core.helpers.CoreHelpers.{ex, sec}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.concurrent.Future
import scala.util.Failure

class TradeHistoryStoreTest_UpdateHistory extends AsyncTestWithControlledTime with TestWithMocks {

  val baseStoreAddPart = new FutureServiceMock[TradeStore, Unit](
    methodCall = _.add(*),
    extendMock = None,
  )

  val baseStoreDeletePart = new FutureServiceMock[TradeStore, Unit](
    methodCall = _.deleteFrom(*),
    extendMock = Some(baseStoreAddPart.instance),
  )

  private lazy val store = new TradeHistoryStore(baseStoreDeletePart.instance)

  private def update(chs: TradeHistorySegment): Future[Unit] = store.updateHistory(chs)

  test("it removes all the trades from the given start from the store") {
    update(tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    ))
    baseStoreDeletePart.verify.deleteFrom(sec(10))
  }

  test("trades after the trades are deleted, the new trades are added") {
    update(tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    ))
    baseStoreAddPart.verifyNever.add(*)
    baseStoreDeletePart.completeNext(())
    baseStoreAddPart.verify.add(Seq(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    ))
  }

  test("when the segment is empty, no trades are added but trades are deleted from the segment start") {
    update(tradeHistorySegment(sec(10))())
    baseStoreDeletePart.completeNext(())
    baseStoreAddPart.verifyNever.add(*)
    baseStoreDeletePart.verify.deleteFrom(sec(10))
  }

  test("an error when adding the trades is passed along") {
    val f = update(tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    ))
    baseStoreDeletePart.completeNext(())
    baseStoreAddPart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

  test("an error when deleting the trades is passed along") {
    val f = update(tradeHistorySegment(sec(10))(
      trade(sec(11), "A"),
      trade(sec(12), "B"),
    ))
    baseStoreDeletePart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
