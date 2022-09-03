package io.liquirium.connect.poloniex

import io.liquirium.core.Side

import java.time.Instant

/**
 * @param id            trade id
 * @param symbol        The trading symbol, like BTC_USDT
 * @param accountType   SPOT
 * @param orderId       the associated order's id
 * @param side          order's side: BUY, SELL
 * @param `type`        order's type: LIMIT, MARKET, LIMIT_MAKER
 * @param matchRole     MAKER, TAKER
 * @param createTime    trade create time
 * @param price         price for the trade
 * @param quantity      base units for the trade
 * @param amount        quote units for the trade
 * @param feeCurrency   fee currency name
 * @param feeAmount     fee amount
 * @param pageId        A globally unique trade Id that can be used as query param in 'from' field
 * @param clientOrderId Order's clientOrderId
 */

case class PoloniexTrade(
  id: String,
  symbol: String,
  accountType: String,
  orderId: String,
  side: Side,
  `type`: String,
  matchRole: String, //MAKER, TAKER
  createTime: Instant,
  price: BigDecimal,
  quantity: BigDecimal,
  amount: BigDecimal,
  feeCurrency: String,
  feeAmount: BigDecimal,
  pageId: String,
  clientOrderId: String,
)
