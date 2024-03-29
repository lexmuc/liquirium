package io.liquirium.core.helpers

import io.liquirium.connect.TradeBatch
import io.liquirium.core
import io.liquirium.core.Trade.Fees
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.{Market, StringTradeId, Trade, TradeHistorySegment, TradeId}

import java.time.Instant

object TradeHelpers {

  def trade(n: Int): Trade = trade(id = n.toString, price = BigDecimal(n))

  def trade(t: Instant, id: String): Trade = trade(id = id, time = t)

  def trades(n: Int): Seq[Trade] = Seq(trade(n), trade(n + 1))

  def trade(
    id: String = "",
    orderId: Option[String] = None,
    quantity: BigDecimal = BigDecimal("1.0"),
    price: BigDecimal = BigDecimal("1.0"),
    fees: Fees = Seq(),
    market: Market = m(0),
    time: Instant = Instant.ofEpochSecond(0),
  ): Trade = core.Trade(
      id = StringTradeId(id),
      market = market,
      orderId = orderId,
      quantity = quantity,
      price = price,
      fees = fees,
      time = time,
    )

  def tradeId(s: String): TradeId = StringTradeId(s)

  def tradeHistorySegment(start: Instant)(tt: Trade*): TradeHistorySegment =
    TradeHistorySegment.fromForwardTrades(start, tt.toList)

  def tradeBatch(start: Instant, nextBatchStart: Option[Instant] = None)(tt: Trade*): TradeBatch =
    TradeBatch(start, tt, nextBatchStart)

}
