package io.liquirium.connect

import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.TradeHelpers.trade
import io.liquirium.core.{CandleHistorySegment, Order, TradeHistorySegment}
import io.liquirium.core.helpers.{CandleHelpers, MarketHelpers, TestWithMocks, TradeHelpers}
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import io.liquirium.util.async.Scheduler
import io.liquirium.util.async.helpers.FakeScheduler
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, theSameInstanceAs}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class PollingExchangeConnectorTest_Subscriptions extends AsyncTestWithScheduler with TestWithMocks {

  private def makeConnector(
    basicConnector: BasicExchangeConnector,
    scheduler: Scheduler,
    openOrderPollingInterval: FiniteDuration = 1.second,
    candlePollingInterval: FiniteDuration = 1.second,
    candleUpdateOverlapStrategy: CandleUpdateOverlapStrategy = CandleUpdateOverlapStrategy.complete,
    tradePollingInterval: FiniteDuration = 1.second,
    tradeUpdateOverlapStrategy: TradeUpdateOverlapStrategy = TradeUpdateOverlapStrategy.complete,
  ): PollingExchangeConnector =
    new PollingExchangeConnector(
      basicConnector = basicConnector,
      scheduler = scheduler,
      openOrderPollingInterval = openOrderPollingInterval,
      candlePollingInterval = candlePollingInterval,
      candleUpdateOverlapStrategy = candleUpdateOverlapStrategy,
      tradePollingInterval = tradePollingInterval,
      tradeUpdateOverlapStrategy = tradeUpdateOverlapStrategy,
    )

  test("it returns a properly configured polling open order subscription") {
    val scheduler = new FakeScheduler()
    val basicConnector = new FutureServiceMock[BasicExchangeConnector, Set[Order]](_.loadOpenOrders(*))
    val connector = makeConnector(
      basicConnector = basicConnector.instance,
      scheduler = scheduler,
      openOrderPollingInterval = 123.seconds
    )
    val subscription = connector.openOrdersSubscription(MarketHelpers.pair(123))
      .asInstanceOf[PollingOpenOrdersSubscription]
    subscription.pollingInterval shouldEqual 123.seconds
    subscription.scheduler shouldBe theSameInstanceAs(scheduler)
    subscription.loadOrders()
    basicConnector.verify.loadOpenOrders(MarketHelpers.pair(123))
  }

  test("it returns a properly configured polling trade history subscription") {
    val scheduler = new FakeScheduler()
    val basicConnector =
      new FutureServiceMock[BasicExchangeConnector, TradeHistorySegment](_.loadTradeHistory(*, *, *))
    val overlapStrategy = TradeUpdateOverlapStrategy.fixedOverlap(secs(123))
    val connector = makeConnector(
      basicConnector = basicConnector.instance,
      scheduler = scheduler,
      tradePollingInterval = 123.seconds,
      tradeUpdateOverlapStrategy = overlapStrategy,
    )
    val segment = TradeHelpers.tradeHistorySegment(sec(100))(trade(sec(101), "A"))
    val subscription = connector.tradeHistorySubscription(MarketHelpers.pair(123), segment)
      .asInstanceOf[PollingTradeHistorySubscription]
    subscription.pollingInterval shouldEqual 123.seconds
    subscription.scheduler shouldBe theSameInstanceAs(scheduler)
    subscription.initialSegment shouldBe segment
    subscription.updateOverlapStrategy shouldBe overlapStrategy

    subscription.loadSegment(sec(1))
    basicConnector.verify.loadTradeHistory(MarketHelpers.pair(123), sec(1), maybeEnd = None)
  }

  test("it returns a properly configured polling candle history subscription") {
    val scheduler = new FakeScheduler()
    val basicConnector =
      new FutureServiceMock[BasicExchangeConnector, CandleHistorySegment](_.loadCandleHistory(*, *, *, *))
    val overlapStrategy = CandleUpdateOverlapStrategy.numberOfCandles(123)
    val connector = makeConnector(
      basicConnector = basicConnector.instance,
      scheduler = scheduler,
      candlePollingInterval = 123.seconds,
      candleUpdateOverlapStrategy = overlapStrategy,
    )
    val segment = CandleHelpers.candleHistorySegment(sec(100), secs(5))(23)
    val subscription = connector.candleHistorySubscription(MarketHelpers.pair(123), segment)
      .asInstanceOf[PollingCandleHistorySubscription]
    subscription.pollingInterval shouldEqual 123.seconds
    subscription.scheduler shouldBe theSameInstanceAs(scheduler)
    subscription.initialSegment shouldBe segment
    subscription.updateOverlapStrategy shouldBe overlapStrategy

    subscription.loadSegment(sec(20))
    basicConnector.verify.loadCandleHistory(MarketHelpers.pair(123), secs(5), start = sec(20), maybeEnd = None)
  }

}
