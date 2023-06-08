package io.liquirium.connect.binance

import io.liquirium.core._
import io.liquirium.util.NumberPrecision

import java.time.{Duration, Instant}

class BinanceModelConverter(exchangeId: ExchangeId) {

  def convertCandle(c: BinanceCandle): Candle =
    Candle(
      startTime = c.openTime,
      length = Duration.ofMillis(c.closeTime.toEpochMilli - c.openTime.toEpochMilli + 1),
      open = c.open,
      close = c.close,
      high = c.high,
      low = c.low,
      quoteVolume = c.quoteAssetVolume
    )

  def getSymbol(pair: TradingPair): String = pair.base + pair.quote

  def convertOrder(o: BinanceOrder): Order = {
    val sign = if (o.side == Side.Buy) 1 else -1
    Order(
      id = o.id,
      market = getMarket(o.symbol),
      openQuantity = (o.originalQuantity - o.executedQuantity) * sign,
      fullQuantity = o.originalQuantity * sign,
      price = o.price
    )
  }

  def convertTrade(t: BinanceTrade): Trade =
    Trade(
      id = StringTradeId(t.id),
      market = getMarket(t.symbol),
      orderId = Some(t.orderId),
      quantity = if (t.isBuyer) t.quantity else t.quantity * -1,
      price = t.price,
      fees = getFees(t.commission, t.commissionAsset),
      time = t.time
    )

  def getMarket(s: String): Market =
    if (s.length == 6 && s.endsWith("USDT"))
      Market(exchangeId, TradingPair(base = s.substring(0, 2), quote = s.substring(2, 6)))
    else if (s.length == 6)
      Market(exchangeId, TradingPair(base = s.substring(0, 3), quote = s.substring(3, 6)))
    else if (s.endsWith("BTC") || s.endsWith("ETH") || s.endsWith("BNB"))
      Market(exchangeId, TradingPair(base = s.substring(0, s.length - 3), quote = s.substring(s.length - 3)))
    else if (s.endsWith("USDT") || s.endsWith("BUSD"))
      Market(exchangeId, TradingPair(base = s.substring(0, s.length - 4), quote = s.substring(s.length - 4)))
    else
      throw new IllegalArgumentException(s"symbol $s is currently not supported by the model converter")

  private def getFees(amount: BigDecimal, asset: String): Seq[(LedgerRef, BigDecimal)] =
    if (amount == BigDecimal(0)) Seq()
    else Seq(LedgerRef(exchangeId, asset) -> amount)

  def extractTrade(report: BinanceExecutionReport): Option[Trade] =
    report.currentExecutionType match {
      case BinanceExecutionType.TRADE => Some(Trade(
        id = StringTradeId(report.tradeId.get.toString),
        market = getMarket(report.symbol),
        orderId = Some(report.orderId.toString),
        quantity = if (report.side == Side.Buy) report.lastExecutedQuantity else -1 * report.lastExecutedQuantity,
        price = report.lastExecutedPrice,
        fees = getFees(report.commissionAmount, report.commissionAsset getOrElse ""),
        time = Instant.ofEpochMilli(report.transactionTime)
      ))
      case _ => None
    }

  def convertSymbolInfo(bsi: BinanceSymbolInfo): OrderConstraints =
    OrderConstraints(
      pricePrecision = NumberPrecision.MultipleOf(bsi.tickSize),
      quantityPrecision = NumberPrecision.MultipleOf(bsi.stepSize)
    )

}
