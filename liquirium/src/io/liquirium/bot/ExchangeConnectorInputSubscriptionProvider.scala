package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, OrderSnapshotHistoryInput, TradeHistoryInput}
import io.liquirium.connect.ExchangeConnectorWithSubscriptions
import io.liquirium.core.{CandleHistorySegment, TradeHistorySegment}
import io.liquirium.eval.{Input, InputSubscriptionProvider}
import io.liquirium.util.Clock
import io.liquirium.util.async.Subscription

class ExchangeConnectorInputSubscriptionProvider(
  connector: ExchangeConnectorWithSubscriptions,
  clock: Clock,
) extends InputSubscriptionProvider {

  override def apply(input: Input[_]): Option[Subscription[_]] = input match {

    case CandleHistoryInput(market, candleLength, start) if  market.exchangeId == connector.exchangeId =>
      val initialSegment = CandleHistorySegment.empty(start, candleLength)
      Some(connector.candleHistorySubscription(market.tradingPair, initialSegment))

    case TradeHistoryInput(market, start) if  market.exchangeId == connector.exchangeId =>
      val initialSegment = TradeHistorySegment.empty(start)
      Some(connector.tradeHistorySubscription(market.tradingPair, initialSegment))

    case OrderSnapshotHistoryInput(market) if market.exchangeId == connector.exchangeId =>
      Some(
        OpenOrdersHistorySubscription(
          connector.openOrdersSubscription(market.tradingPair),
          clock = clock,
        )
      )

    case _ => None

  }

}
