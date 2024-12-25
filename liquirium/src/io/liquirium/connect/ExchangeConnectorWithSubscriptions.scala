package io.liquirium.connect

import io.liquirium.core.{CandleHistorySegment, Order, TradeHistorySegment, TradingPair}
import io.liquirium.util.async.Subscription

trait ExchangeConnectorWithSubscriptions extends BasicExchangeConnector {

  def candleHistorySubscription(
    tradingPair: TradingPair,
    initialSegment: CandleHistorySegment,
  ): Subscription[CandleHistorySegment]

  def tradeHistorySubscription(
    tradingPair: TradingPair,
    initialSegment: TradeHistorySegment,
  ): Subscription[TradeHistorySegment]

  def openOrdersSubscription(tradingPair: TradingPair): Subscription[Set[Order]]

}

