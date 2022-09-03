package io.liquirium.connect.poloniex

import io.liquirium.core.Side
import play.api.libs.json.{JsObject, JsValue}

import java.time.Instant

class PoloniexJsonConverter {

  def convertSingleCandle(v: JsValue): PoloniexCandle = PoloniexCandle(
    low = (v \ 0).get.as[BigDecimal],
    high = (v \ 1).get.as[BigDecimal],
    open = (v \ 2).get.as[BigDecimal],
    close = (v \ 3).get.as[BigDecimal],
    amount = (v \ 4).get.as[BigDecimal],
    quantity = (v \ 5).get.as[BigDecimal],
    buyTakerAmount = (v \ 6).get.as[BigDecimal],
    buyTakerQuantity = (v \ 7).get.as[BigDecimal],
    tradeCount = (v \ 8).get.as[Int],
    ts = Instant.ofEpochMilli((v \ 9).get.as[Long]),
    weightedAverage = (v \ 10).get.as[BigDecimal],
    interval = PoloniexCandleLength.forCode((v \ 11).get.as[String]),
    startTime = Instant.ofEpochMilli((v \ 12).get.as[Long]),
    closeTime = Instant.ofEpochMilli((v \ 13).get.as[Long]),
  )

  def convertCandles(v: JsValue): Seq[PoloniexCandle] = {
    val result = v.as[Seq[JsValue]].map(convertSingleCandle)

    //#? Sollte man testen, ob dieser test mit der neuen Api hinfällig ist?
    if (result.size == 1 && result.head.startTime == Instant.ofEpochSecond(0)) Seq()
    else result
  }


  def convertTrade(v: JsValue): PoloniexTrade = {
    PoloniexTrade(
      id = v("id").as[String],
      symbol = v("symbol").as[String],
      accountType = v("accountType").as[String],
      orderId = v("orderId").as[String],
      side = convertSide(v("side").as[String]),
      `type` = v("type").as[String],
      matchRole = v("matchRole").as[String],
      createTime = Instant.ofEpochMilli(v("createTime").as[Long]),
      price = v("price").as[BigDecimal],
      quantity = v("quantity").as[BigDecimal],
      amount = v("amount").as[BigDecimal],
      feeCurrency = v("feeCurrency").as[String],
      feeAmount = v("feeAmount").as[BigDecimal],
      pageId = v("pageId").as[String],
      clientOrderId = v("clientOrderId").as[String],
    )
  }

  def convertTrades(v: JsValue): Seq[PoloniexTrade] =
    v.as[Seq[JsObject]].map(t => convertTrade(t))


  def convertOrder(v: JsValue): PoloniexOrder =
    PoloniexOrder(
      id = v("id").as[String],
      clientOrderId = v("clientOrderId").as[String],
      symbol = v("symbol").as[String],
      state = v("state").as[String],
      accountType = v("accountType").as[String],
      side = convertSide(v("side").as[String]),
      `type` = v("type").as[String],
      timeInForce = v("timeInForce").as[String],
      quantity = v("quantity").as[BigDecimal],
      price = v("price").as[BigDecimal],
      avgPrice = v("avgPrice").as[BigDecimal],
      amount = v("amount").as[BigDecimal],
      filledQuantity = v("filledQuantity").as[BigDecimal],
      filledAmount = v("filledAmount").as[BigDecimal],
      createTime = Instant.ofEpochMilli(v("createTime").as[Long]),
      updateTime = Instant.ofEpochMilli(v("updateTime").as[Long]),
    )

  def convertOrders(v: JsValue): Seq[PoloniexOrder] =
    v.as[Seq[JsObject]].map(t => convertOrder(t))

  def convertCreateOrderResponse(v: JsValue): PoloniexCreateOrderResponse = {
    PoloniexCreateOrderResponse(
      id = v("id").as[String],
      clientOrderId = v("clientOrderId").as[String],
    )
  }

  def convertCancelOrderByIdResponse(v: JsValue): PoloniexCancelOrderByIdResponse = {
    PoloniexCancelOrderByIdResponse(
      orderId = v("orderId").as[String],
      clientOrderId = v("clientOrderId").as[String],
      state = v("state").as[String],
      code = v("code").as[Int],
      message = v("message").as[String],
    )
  }

  private def convertSide(s: String): Side = s match {
    case "BUY" => Side.Buy
    case "SELL" => Side.Sell
    case _ => throw new RuntimeException(s"Invalid side value: $s")
  }

  def convertSymbolInfo(v: JsValue): PoloniexSymbolInfo = {
    val symbol = (v \ "symbol").as[String]
    val symbolTradeLimit = (v \ "symbolTradeLimit").as[JsValue]
    val priceScale = (symbolTradeLimit \ "priceScale").as[Int]
    val quantityScale = (symbolTradeLimit \ "quantityScale").as[Int]
    val minAmount = BigDecimal((symbolTradeLimit \ "minAmount").as[String])
    val minQuantity = BigDecimal((symbolTradeLimit \ "minQuantity").as[String])

    PoloniexSymbolInfo(
      symbol = symbol,
      priceScale = priceScale,
      quantityScale = quantityScale,
      minAmount = minAmount,
      minQuantity = minQuantity,
    )
  }

  def convertSymbolInfos(v: JsValue): Seq[PoloniexSymbolInfo] = {
    v.as[Seq[JsValue]].map(convertSymbolInfo)
  }

}
