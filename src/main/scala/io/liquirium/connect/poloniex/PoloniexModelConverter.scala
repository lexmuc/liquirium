package io.liquirium.connect.poloniex

import io.liquirium.core._
import io.liquirium.util.NumberPrecision

class PoloniexModelConverter(exchangeId: ExchangeId) {

  def convertCandle(c: PoloniexCandle): Candle = Candle(
    startTime = c.startTime,
    length = c.interval.length,
    open = c.open,
    close = c.close,
    high = c.high,
    low = c.low,
    quoteVolume = c.amount,
  )

  def convertTrade(pt: PoloniexTrade): Trade = {
    val market = getMarket(pt.symbol)
    Trade(
      id = StringTradeId(pt.id),
      market = market,
      orderId = Option(pt.orderId),
      quantity = if (pt.side == Side.Buy) pt.quantity else pt.quantity * -1,
      price = pt.price,
      fees = Seq(LedgerRef(market.exchangeId, pt.feeCurrency) -> pt.feeAmount),
      time = pt.createTime
    )
  }

  def convertOrder(po: PoloniexOrder): Order = Order(
    id = po.id,
    market = getMarket(po.symbol),
    openQuantity = (po.quantity - po.filledQuantity) * (if (po.side == Side.Buy) 1 else -1),
    fullQuantity = if (po.side == Side.Buy) po.quantity else po.quantity * -1,
    price = po.price
  )

  def getMarket(s: String): Market = {
    val pair = TradingPair(s.substring(0, s.indexOf("_")), s.substring(s.indexOf("_") + 1))
    Market(exchangeId, pair)
  }

  def getSymbol(pair: TradingPair): String = s"${ pair.base }_${ pair.quote }"

  def convertSymbolInfo(psi: PoloniexSymbolInfo): OrderConstraints =
    OrderConstraints(
      pricePrecision = NumberPrecision.digitsAfterSeparator(psi.priceScale),
      orderQuantityPrecision = NumberPrecision.digitsAfterSeparator(psi.quantityScale),
    )

}
