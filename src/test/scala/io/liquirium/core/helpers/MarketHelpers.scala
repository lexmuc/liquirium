package io.liquirium.core.helpers

import io.liquirium.core
import io.liquirium.core.helpers.CoreHelpers.{asset, dec}
import io.liquirium.core._

object MarketHelpers {

  def market(s: String): Market = core.Market(ExchangeId(s), TradingPair("b" + s, "q" + s))

  def market(exchangeId: String, p: TradingPair): Market = core.Market(ExchangeId(exchangeId), p)

  def market(exchangeId: String, n: Int): Market = core.Market(ExchangeId(exchangeId), pair(n))

  def market(e: ExchangeId, n: Int): Market = core.Market(e, pair(n))

  def market(e: ExchangeId, base: String, quote: String): Market = core.Market(e, pair(base, quote))

  def market(n: Int): Market = market(n.toString)

  def pair(base: String, quote: String): TradingPair = TradingPair(base, quote)

  def pair(n: Int): TradingPair = TradingPair("b" + n.toString, "q" + n.toString)

  def m(n: Int): Market = market(n)

  def m(s: String): Market = market(s)

  def assetMarket(b: String, q: String): AssetMarket = AssetMarket(base = asset(b), quote = asset(q))

  def eid(n: Int): ExchangeId = ExchangeId(n.toString)

  def eid(s: String): ExchangeId = exchangeId(s)

  def exchangeId(s: String): ExchangeId = ExchangeId(s)

  def orderConstraints(
    pricePrecision: PricePrecision = PricePrecision.MultipleOf(dec(1)),
    orderQuantityPrecision: OrderQuantityPrecision = OrderQuantityPrecision.MultipleOf(dec(1)),
  ): OrderConstraints = OrderConstraints(
    pricePrecision = pricePrecision,
    orderQuantityPrecision = orderQuantityPrecision,
  )

  def orderConstraints(n: Int): OrderConstraints = orderConstraints(pricePrecision = PricePrecision.MultipleOf(dec(n)))

}
