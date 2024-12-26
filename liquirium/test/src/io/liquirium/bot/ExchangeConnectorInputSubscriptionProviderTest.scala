package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, TradeHistoryInput}
import io.liquirium.connect.ExchangeConnectorWithSubscriptions
import io.liquirium.core.{CandleHistorySegment, ExchangeId, Market, TradeHistorySegment}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.{MarketHelpers, TestWithMocks}
import io.liquirium.eval.helpers.EvalHelpers
import io.liquirium.util.async.Subscription
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper


class ExchangeConnectorInputSubscriptionProviderTest extends TestWithMocks {
  private val exchangeId: ExchangeId = MarketHelpers.exchangeId("SOME_ID")

  private val connector = mock(classOf[ExchangeConnectorWithSubscriptions])
  connector.exchangeId returns exchangeId

  private val provider = new ExchangeConnectorInputSubscriptionProvider(connector)

  test("it returns None for unknown inputs") {
    val input = EvalHelpers.input(1)
    provider(input) shouldEqual None
  }

  test("it returns a proper subscription for the candle history") {
    val provider = new ExchangeConnectorInputSubscriptionProvider(connector)
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
    val provider = new ExchangeConnectorInputSubscriptionProvider(connector)
    val input = CandleHistoryInput(
      market = Market(otherId, MarketHelpers.pair(123)),
      candleLength = secs(10),
      start = sec(100),
    )
    provider.apply(input) shouldEqual None
  }

  test("it returns a proper subscription for a trade history input") {
    val provider = new ExchangeConnectorInputSubscriptionProvider(connector)
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
    val provider = new ExchangeConnectorInputSubscriptionProvider(connector)
    val input = TradeHistoryInput(
      market = Market(otherId, MarketHelpers.pair(123)),
      start = sec(100),
    )
    provider.apply(input) shouldEqual None
  }

}
