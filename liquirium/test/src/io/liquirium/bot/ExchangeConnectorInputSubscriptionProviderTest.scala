package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, OrderSnapshotHistoryInput, TradeHistoryInput}
import io.liquirium.connect.ExchangeConnectorWithSubscriptions
import io.liquirium.core.{CandleHistorySegment, ExchangeId, Market, Order, TradeHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.{MarketHelpers, TestWithMocks}
import io.liquirium.eval.helpers.EvalHelpers
import io.liquirium.helpers.FakeClock
import io.liquirium.util.async.Subscription
import org.mockito.Mockito.mock
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper


class ExchangeConnectorInputSubscriptionProviderTest extends TestWithMocks {
  private val exchangeId: ExchangeId = MarketHelpers.exchangeId("SOME_ID")

  private val connector = mock(classOf[ExchangeConnectorWithSubscriptions])
  connector.exchangeId returns exchangeId
  private val clock = new FakeClock(sec(1))

  private val provider = new ExchangeConnectorInputSubscriptionProvider(connector, clock)

  test("it returns None for unknown inputs") {
    val input = EvalHelpers.input(1)
    provider(input) shouldEqual None
  }

  test("it returns a proper subscription for the candle history") {
    val market = Market(exchangeId, MarketHelpers.pair(123))
    val input = CandleHistoryInput(
      market = market,
      candleLength = secs(10),
      start = sec(100),
    )
    val initialSegment = CandleHistorySegment.empty(
      start = sec(100),
      candleLength = secs(10),
    )
    val subscription = mock(classOf[Subscription[CandleHistorySegment]])
    connector.candleHistorySubscription(MarketHelpers.pair(123), initialSegment) returns subscription
    provider.apply(input) shouldEqual Some(subscription)
  }

  test("it returns None for a candle history input with another exchange id") {
    val otherId = MarketHelpers.exchangeId("OTHER")
    val input = CandleHistoryInput(
      market = Market(otherId, MarketHelpers.pair(123)),
      candleLength = secs(10),
      start = sec(100),
    )
    provider.apply(input) shouldEqual None
  }

  test("it returns a proper subscription for a trade history input") {
    val market = Market(exchangeId, MarketHelpers.pair(123))
    val input = TradeHistoryInput(
      market = market,
      start = sec(100),
    )
    val initialSegment = TradeHistorySegment.empty( start = sec(100))
    val subscription = mock(classOf[Subscription[TradeHistorySegment]])
    connector.tradeHistorySubscription(MarketHelpers.pair(123), initialSegment) returns subscription
    provider.apply(input) shouldEqual Some(subscription)
  }

  test("it returns None for a trade history input with another exchange id") {
    val otherId = MarketHelpers.exchangeId("OTHER")
    val input = TradeHistoryInput(
      market = Market(otherId, MarketHelpers.pair(123)),
      start = sec(100),
    )
    provider.apply(input) shouldEqual None
  }

  test("it returns an OpenOrderHistorySubscription that is based on the open orders subscription of the exchange") {
    val input = OrderSnapshotHistoryInput(
      market = Market(exchangeId, MarketHelpers.pair(123)),
    )
    val ordersSubscription = mock(classOf[Subscription[Set[Order]]])
    connector.openOrdersSubscription(MarketHelpers.pair(123)) returns ordersSubscription
    provider.apply(input) shouldEqual Some(
      OpenOrdersHistorySubscription(ordersSubscription, clock)
    )
  }

  test("it returns None when open orders for another exchange are requested") {
    val otherId = MarketHelpers.exchangeId("OTHER")
    val input = OrderSnapshotHistoryInput(
      market = Market(otherId, MarketHelpers.pair(123)),
    )
    provider.apply(input) shouldEqual None
  }

}
