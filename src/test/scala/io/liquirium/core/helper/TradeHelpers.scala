package io.liquirium.core.helper

import io.liquirium.core
import io.liquirium.core.Trade.Fees
import io.liquirium.core.helper.MarketHelpers.m
import io.liquirium.core.{Market, Trade}

import java.time.Instant

object TradeHelpers {

  def trade(n: Int): Trade = trade(id = n.toString, price = BigDecimal(n))

  def trades(n: Int) = Seq(trade(n), trade(n + 1))

  def trade(
    id: String = "",
    orderId: Option[String] = None,
    quantity: BigDecimal = BigDecimal("1.0"),
    price: BigDecimal = BigDecimal("1.0"),
    fees: Fees = Seq(),
    market: Market = m(0),
    time: Instant = Instant.ofEpochSecond(0)
  ): Trade = core.Trade(
      id = id,
      market = market,
      orderId = orderId,
      quantity = quantity,
      price = price,
      fees = fees,
      time = time
    )

}
