package io.liquirium.connect

import io.liquirium.bot.helpers.OperationRequestHelpers.operationRequest
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import io.liquirium.core.helpers.{MarketHelpers, TestWithMocks}
import io.liquirium.core.{CandleHistorySegment, OperationRequestSuccessResponse, Order, TradeHistorySegment}
import io.liquirium.util.async.helpers.FakeScheduler
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, theSameInstanceAs}

import scala.concurrent.duration.DurationInt

class PollingExchangeConnectorTest_ForwardToBasicConnector extends AsyncTestWithScheduler with TestWithMocks {

  private def makeConnector(bc: BasicExchangeConnector): PollingExchangeConnector =
    new PollingExchangeConnector(
      basicConnector = bc,
      scheduler = new FakeScheduler(),
      openOrderPollingInterval = 1.second,
      candlePollingInterval = 1.second,
      tradePollingInterval = 1.second,
      tradeUpdateOverlapStrategy = TradeUpdateOverlapStrategy.complete,
      candleUpdateOverlapStrategy = CandleUpdateOverlapStrategy.complete,
    )

  test("an open order request is simply forwarded") {
    val basicConnector = new FutureServiceMock[BasicExchangeConnector, Set[Order]](_.loadOpenOrders(*))
    val future = makeConnector(basicConnector.instance).loadOpenOrders(MarketHelpers.pair(1))
    basicConnector.verify.loadOpenOrders(MarketHelpers.pair(1))
    future shouldBe theSameInstanceAs(basicConnector.lastReturnedFuture())
  }

  test("a trade history request is simply forwarded") {
    val basicConnector =
      new FutureServiceMock[BasicExchangeConnector, TradeHistorySegment](_.loadTradeHistory(*, *, *))
    val future = makeConnector(basicConnector.instance)
      .loadTradeHistory(MarketHelpers.pair(1), sec(100), Some(sec(110)))
    basicConnector.verify.loadTradeHistory(MarketHelpers.pair(1), sec(100), Some(sec(110)))
    future shouldBe theSameInstanceAs(basicConnector.lastReturnedFuture())
  }

  test("a candle history request is simply forwarded") {
    val basicConnector =
      new FutureServiceMock[BasicExchangeConnector, CandleHistorySegment](_.loadCandleHistory(*, *, *, *))
    val connector = makeConnector(basicConnector.instance)
    val future = connector.loadCandleHistory(MarketHelpers.pair(1), secs(2), sec(100), Some(sec(110)))
    basicConnector.verify.loadCandleHistory(MarketHelpers.pair(1), secs(2), sec(100), Some(sec(110)))
    future shouldBe theSameInstanceAs(basicConnector.lastReturnedFuture())
  }

  test("a trade request is simply forwarded") {
    val basicConnector =
      new FutureServiceMock[BasicExchangeConnector, OperationRequestSuccessResponse[_]](_.submitRequest(*))
    val connector = makeConnector(basicConnector.instance)
    val future = connector.submitRequest(operationRequest(1))
    basicConnector.verify.submitRequest(operationRequest(1))
    future shouldBe theSameInstanceAs(basicConnector.lastReturnedFuture())
  }

}
