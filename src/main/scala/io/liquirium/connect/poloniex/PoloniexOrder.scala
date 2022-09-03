package io.liquirium.connect.poloniex

import io.liquirium.core.Side

import java.time.Instant

object PoloniexOrder {

  sealed trait OrderType

  object OrderType {
    case object Market extends OrderType

    case object Limit extends OrderType

    case object LimitMaker extends OrderType
  }

}

/**
 * @param id            order id
 * @param clientOrderId User specified id
 * @param symbol        The symbol to trade,like BTC_USDT
 * @param state         FAILED, FILLED, CANCELED, PARTIALLY_CANCELED
 * @param accountType   SPOT
 * @param side          BUY, SELL
 * @param `type`        MARKET, LIMIT, LIMIT_MAKER
 * @param timeInForce   GTC, IOC, FOK
 * @param quantity      base units for the order
 * @param price
 * @param avgPrice      avgPrice = filledAmunt/filledQuantity
 * @param amount        quote units for the order
 * @param filledQuantity
 * @param filledAmount
 * @param createTime
 * @param updateTime
 */

case class PoloniexOrder(
  id: String,
  clientOrderId: String,
  symbol: String,
  state: String,
  accountType: String,
  side: Side,
  `type`: String,
  timeInForce: String,
  quantity: BigDecimal,
  price: BigDecimal,
  avgPrice: BigDecimal,
  amount: BigDecimal,
  filledQuantity: BigDecimal,
  filledAmount: BigDecimal,
  createTime: Instant,
  updateTime: Instant
)
