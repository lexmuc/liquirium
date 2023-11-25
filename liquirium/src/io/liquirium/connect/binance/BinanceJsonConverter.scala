package io.liquirium.connect.binance

import io.liquirium.connect.binance
import io.liquirium.core.Side
import play.api.libs.json.{JsNull, JsValue}

import java.time.Instant

class BinanceJsonConverter() {

  def convertSingleCandle(v: JsValue): BinanceCandle =
    binance.BinanceCandle(
      openTime = Instant.ofEpochMilli((v \ 0).get.as[Long]),
      open = (v \ 1).get.as[BigDecimal],
      high = (v \ 2).get.as[BigDecimal],
      low = (v \ 3).get.as[BigDecimal],
      close = (v \ 4).get.as[BigDecimal],
      quoteAssetVolume = (v \ 7).get.as[BigDecimal],
      closeTime = Instant.ofEpochMilli((v \ 6).get.as[Long])
    )

  def convertCandles(v: JsValue): Seq[BinanceCandle] = v.as[Seq[JsValue]].map(convertSingleCandle)

  def convertSingleOrder(v: JsValue): BinanceOrder =
    binance.BinanceOrder(
      id = v("orderId").as[Long].toString,
      symbol = v("symbol").as[String],
      clientOrderId = v("clientOrderId").as[String],
      price = BigDecimal(v("price").as[String]),
      originalQuantity = BigDecimal(v("origQty").as[String]),
      executedQuantity = BigDecimal(v("executedQty").as[String]),
      `type` = v("type").as[String],
      side = if (v("side").as[String] == "BUY") Side.Buy else Side.Sell,
    )

  def convertOrders(v: JsValue): Seq[BinanceOrder] = v.as[Seq[JsValue]].map(convertSingleOrder)

  def convertTrades(v: JsValue): Seq[BinanceTrade] = v.as[Seq[JsValue]].map(convertSingleTrade)

  def convertSingleTrade(v: JsValue): BinanceTrade =
    binance.BinanceTrade(
      id = v("id").as[Long].toString,
      symbol = v("symbol").as[String],
      orderId = v("orderId").as[Long].toString,
      price = BigDecimal(v("price").as[String]),
      quantity = BigDecimal(v("qty").as[String]),
      commission = BigDecimal(v("commission").as[String]),
      commissionAsset = v("commissionAsset").as[String],
      time = Instant.ofEpochMilli(v("time").as[Long]),
      isBuyer = v("isBuyer").as[Boolean]
    )

  def convertPayload(v: JsValue): BinanceStreamPayload =
    v("e").as[String] match {
      case "executionReport" => BinanceExecutionReport(
        eventTime = v("E").as[Long],
        symbol = v("s").as[String],
        clientOrderId = v("c").as[String],
        side = if (v("S").as[String] == "BUY") Side.Buy else Side.Sell,
        orderType = v("o").as[String],
        orderQuantity = BigDecimal(v("q").as[String]),
        orderPrice = BigDecimal(v("p").as[String]),
        currentExecutionType = executionType(v("x").as[String]),
        orderId = v("i").as[Long],
        lastExecutedQuantity = BigDecimal(v("l").as[String]),
        lastExecutedPrice = BigDecimal(v("L").as[String]),
        commissionAmount = BigDecimal(v("n").as[String]),
        commissionAsset = if (v("N") == JsNull) None else Some(v("N").as[String]),
        transactionTime = v("T").as[Long],
        tradeId = if (v("t").as[Long] == -1) None else Some(v("t").as[Long]),
      )
      case e => IrrelevantPayload(e)
    }

  private def executionType(s: String): BinanceExecutionType = s match {
    case "NEW" => BinanceExecutionType.NEW
    case "CANCELED" => BinanceExecutionType.CANCELED
    case "TRADE" => BinanceExecutionType.TRADE
    case "REPLACED" => BinanceExecutionType.REPLACED
    case "REJECTED" => BinanceExecutionType.REJECTED
    case "EXPIRED" => BinanceExecutionType.EXPIRED
    case _ => throw new RuntimeException("Unknown execution type: " + s)
  }

  def convertOrderType(bot: BinanceOrderType): String = bot match {
    case BinanceOrderType.LIMIT => "LIMIT"
    case BinanceOrderType.MARKET => "MARKET"
    case BinanceOrderType.STOP_LOSS => "STOP_LOSS"
    case BinanceOrderType.STOP_LOSS_LIMIT => "STOP_LOSS_LIMIT"
    case BinanceOrderType.TAKE_PROFIT => "TAKE_PROFIT"
    case BinanceOrderType.TAKE_PROFIT_LIMIT => "TAKE_PROFIT_LIMIT"
    case BinanceOrderType.LIMIT_MAKER => "LIMIT_MAKER"
  }

  def convertSymbolInfo(v: JsValue): BinanceSymbolInfo = {
    val symbol = v("symbol").as[String]
    val filters = v("filters").as[Seq[JsValue]]
    val priceFilter = filters.find(v => v("filterType").as[String] == "PRICE_FILTER").get
    val tickSize = BigDecimal(priceFilter("tickSize").as[String])
    val minPrice = BigDecimal(priceFilter("minPrice").as[String])
    val maxPrice = BigDecimal(priceFilter("maxPrice").as[String])
    val lotSize = filters.find(v => v("filterType").as[String] == "LOT_SIZE").get
    val stepSize = BigDecimal(lotSize("stepSize").as[String])
    val minQuantity = BigDecimal(lotSize("minQty").as[String])
    val maxQuantity = BigDecimal(lotSize("maxQty").as[String])

    BinanceSymbolInfo(
      symbol = symbol,
      tickSize = tickSize,
      minPrice = minPrice,
      maxPrice = maxPrice,
      stepSize = stepSize,
      minQuantity = minQuantity,
      maxQuantity = maxQuantity,
    )
  }

  def convertSymbolInfos(v: JsValue): Seq[BinanceSymbolInfo] = {
    v.as[Seq[JsValue]].map(convertSymbolInfo)
  }

}
