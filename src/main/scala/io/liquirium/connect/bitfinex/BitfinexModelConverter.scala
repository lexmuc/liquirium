package io.liquirium.connect.bitfinex

import io.liquirium.core
import io.liquirium.core._

import java.time.Duration

class BitfinexModelConverter(exchangeId: ExchangeId) {

  def genericOrderId(id: Long): String = id.toString

  def convertOrder(o: BitfinexOrder): Order = core.Order(
    id = o.id.toString,
    market = getMarket(o.symbol),
    openQuantity = o.amount,
    fullQuantity = o.originalAmount,
    price = o.price
  )

  def convertTrade(t: BitfinexTrade): Trade = core.Trade(
    id = StringTradeId(t.id.toString),
    market = getMarket(t.symbol),
    orderId = Some(t.orderId.toString),
    quantity = t.amount,
    price = t.price,
    fees =
      if (t.fee == BigDecimal(0)) Seq()
      else Seq(LedgerRef(exchangeId, t.feeCurrency) -> t.fee * BigDecimal(-1)),
    time = t.timestamp
  )

  def getMarketFromPair(pair: String): Market = {
    val tradingPair =
      if (pair.length == 6 && !pair.contains(":")) TradingPair(pair.substring(0, 3), pair.substring(3, 6))
      else if (pair.contains(":")) TradingPair(pair.split(":")(0), pair.split(":")(1))
      else TradingPair("???", "???")
    Market(exchangeId, tradingPair)
  }

  def getMarket(symbol: String): Market = {
    val pair =
      if (symbol.startsWith("t") && symbol.length == 7) TradingPair(symbol.substring(1, 4), symbol.substring(4, 7))
      else if (symbol.startsWith("t") && symbol.contains("F0:")) convertDerivativesSymbol(symbol)
      else if (symbol.startsWith("f") && symbol.length == 4) TradingPair("FUNDING", symbol.substring(1, 4))
      else TradingPair("???", "???")
    core.Market(exchangeId, pair)
  }

  private def convertDerivativesSymbol(s: String): TradingPair = {
    val parts = s.drop(1).split(":")
    TradingPair(parts(0), parts(1))
  }

  def getSymbol(pair: TradingPair): String = {
    if (pair.base.length >= 5 && pair.base.endsWith("F0") && pair.quote.length >= 5 && pair.quote.endsWith("F0"))
      "t" + pair.base + ":" + pair.quote
    else "t" + pair.base + pair.quote
  }

  def convertCandle(c: BitfinexCandle, length: Duration): Candle =
    Candle(
      startTime = c.timestamp,
      length = length,
      open = c.open,
      close = c.close,
      high = c.high,
      low = c.low,
      quoteVolume = (c.high + c.low) / BigDecimal(2) * c.volume
    )

  def convertPairInfo(bpi: BitfinexPairInfo): OrderConstraints = {
    OrderConstraints(
      pricePrecision = PricePrecision.significantDigits(5, Some(8)),
      orderQuantityPrecision = OrderQuantityPrecision.DigitsAfterSeparator(8)
    )
  }
}
