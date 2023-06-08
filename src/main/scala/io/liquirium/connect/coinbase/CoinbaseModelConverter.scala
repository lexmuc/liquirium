package io.liquirium.connect.coinbase

import io.liquirium.core._

import java.time.Duration

class CoinbaseModelConverter(exchangeId: ExchangeId) {

  def getProductId(pair: TradingPair): String = {
    pair.base + "-" + pair.quote
  }

  def getMarket(productId: String): Market = {
    val pair = TradingPair(
      productId.substring(0, productId.indexOf("-")),
      productId.substring(productId.indexOf("-") + 1),
    )
    Market(exchangeId, pair)
  }

  def convertCandle(c: CoinbaseCandle, candleLength: Duration): Candle =
    Candle(
      startTime = c.start,
      length = candleLength,
      open = c.open,
      close = c.close,
      high = c.high,
      low = c.low,
      quoteVolume = c.volume
    )

  def convertOrder(co: CoinbaseOrder): Order =
    Order(
      id = co.orderId,
      market = getMarket(co.productId),
      openQuantity = co.openQuantity * (if (co.side == Side.Buy) 1 else -1),
      fullQuantity = co.fullQuantity * (if (co.side == Side.Buy) 1 else -1),
      price = co.price,
    )

  def convertTrade(ct: CoinbaseTrade): Trade = {
    //Todo: fee currency ???
    val market = getMarket(ct.productId)
    Trade(
      id = StringTradeId(ct.tradeId),
      market = getMarket(ct.productId),
      orderId = Option(ct.orderId),
      quantity = ct.size * (if (ct.side == Side.Buy) 1 else -1),
      price = ct.price,
      fees = Seq(),
      time = ct.tradeTime,
    )

  }

  def convertProductInfo(cpi: CoinbaseProductInfo): OrderConstraints =
    OrderConstraints(
      pricePrecision = NumberPrecision.MultipleOf(cpi.quoteIncrement),
      orderQuantityPrecision = NumberPrecision.MultipleOf(cpi.baseIncrement),
    )

}
