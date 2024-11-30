package io.liquirium.util.store

import io.liquirium.connect.TradeBatch
import io.liquirium.core.TradeHistorySegment
import io.liquirium.core.helpers.CoreHelpers.{ex, sec}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeBatch, tradeHistorySegment}
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant
import scala.concurrent.Future
import scala.util.{Failure, Success}

class TradeHistoryStoreTest_LoadHistory extends AsyncTestWithControlledTime with TestWithMocks {

  val baseLoader: TradeStore = mock(classOf[TradeStore])

  val baseStoreLoaderPart = new FutureServiceMock[TradeStore, TradeBatch](
    methodCall = _.get(*, *),
    extendMock = Some(baseLoader),
  )

  private lazy val store = new TradeHistoryStore(baseStoreLoaderPart.instance)

  private def load(start: Instant, inspectionTime: Option[Instant] = None): Future[TradeHistorySegment] =
    store.loadHistory(start, inspectionTime)

  test("for a given start it requests all trades from this point on from the base store") {
    load(sec(100))
    baseStoreLoaderPart.verify.get(from = Some(sec(100)), until = None)
  }

  test("if an inspection time is given the trades are only requested up to this time") {
    load(sec(100), Some(sec(200)))
    baseStoreLoaderPart.verify.get(from = Some(sec(100)), until = Some(sec(200)))
  }

  test("the trades are returned as a candle history segment") {
    val f = load(sec(100))
    baseStoreLoaderPart.completeNext(
      tradeBatch(sec(100))(
        trade(sec(101), "A"),
        trade(sec(102), "B"),
      )
    )
    f.value.get shouldEqual Success(tradeHistorySegment(sec(100))(
      trade(sec(101), "A"),
      trade(sec(102), "B"),
    ))
  }

  test("when there are no trades, the empty segment is returned") {
    val f = load(sec(100))
    baseStoreLoaderPart.completeNext(tradeBatch(sec(100))())
    f.value.get shouldEqual Success(tradeHistorySegment(sec(100))())
  }

  test("an error when getting the trades is passed along") {
    val f = load(sec(100))
    baseStoreLoaderPart.failNext(ex(123))
    f.value.get shouldEqual Failure(ex(123))
  }

}
