package io.liquirium.connect.coinbase

import io.liquirium.core.Side
import play.api.libs.json.JsValue

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.{Locale, TimeZone}

class CoinbaseJsonConverter {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH)
  dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))

  def convertSingleCandle(v: JsValue): CoinbaseCandle = CoinbaseCandle(
    start = Instant.ofEpochMilli(v("start").as[String].toLong),
    low = BigDecimal(v("low").as[String]),
    high = BigDecimal(v("high").as[String]),
    open = BigDecimal(v("open").as[String]),
    close = BigDecimal(v("close").as[String]),
    volume = BigDecimal(v("volume").as[String]),
  )

  def convertCandles(v: JsValue): Seq[CoinbaseCandle] = v.as[Seq[JsValue]].map(convertSingleCandle)

  def convertSingleOrder(v: JsValue): CoinbaseOrder = CoinbaseOrder(
    orderId = v("order_id").as[String],
    productId = v("product_id").as[String],
    fullQuantity = BigDecimal((v \ "order_configuration" \ "limit_limit_gtc" \ "base_size").as[String]),
    filledQuantity = BigDecimal((v \ "filled_size").as[String]),
    side = convertSide(v("side").as[String]),
    price = BigDecimal((v \ "order_configuration" \ "limit_limit_gtc" \ "limit_price").as[String]),
  )

  def convertOrders(v: JsValue): Seq[CoinbaseOrder] = v.as[Seq[JsValue]].map(convertSingleOrder)

  def convertCreateOrderResponse(v: JsValue): CoinbaseCreateOrderResponse = {
    val isSuccess = v("success").as[Boolean]
    if (isSuccess) {
      CoinbaseCreateOrderResponse.Success(
        orderId = (v \ "success_response" \ "order_id").as[String],
        clientOrderId = (v \ "success_response" \ "client_order_id").as[String],
      )
    } else {
      CoinbaseCreateOrderResponse.Failure(
        error = (v \ "error_response" \ "error").as[String],
        message = (v \ "error_response" \ "message").as[String],
        details = (v \ "error_response" \ "error_details").as[String],
      )
    }
  }

  def convertSingleCancelOrderResult(v: JsValue): CoinbaseCancelOrderResult =
    CoinbaseCancelOrderResult(
      success = (v \ "success").as[Boolean],
      failureReason = (v \ "failure_reason").as[String],
      orderId = (v \ "order_id").as[String],
    )

  def convertCancelOrderResults(v: JsValue): Seq[CoinbaseCancelOrderResult] = {
    v.as[Seq[JsValue]].map(convertSingleCancelOrderResult)
  }


  def convertSingleTrade(v: JsValue): CoinbaseTrade = CoinbaseTrade(
    entryId = v("entry_id").as[String],
    tradeId = v("trade_id").as[String],
    orderId = v("order_id").as[String],
    tradeTime = dateFormat.parse(v("trade_time").as[String]).toInstant,
    tradeType = checkTradeType(v("trade_type").as[String]),
    price = BigDecimal(v("price").as[String]),
    size = BigDecimal(v("size").as[String]),
    commission = BigDecimal(v("commission").as[String]),
    productId = v("product_id").as[String],
    sequenceTimestamp = dateFormat.parse(v("sequence_timestamp").as[String]).toInstant,
    side = convertSide(v("side").as[String]),
  )

  private def checkTradeType(tt: String): String =
    tt match {
      case "FILL" => tt
      case _ => throw new RuntimeException("trade has other trade type than 'FILL'")
    }

  def convertTrades(v: JsValue): Seq[CoinbaseTrade] = v.as[Seq[JsValue]].map(convertSingleTrade)

  private def convertSide(s: String): Side = s match {
    case "BUY" => Side.Buy
    case "SELL" => Side.Sell
    case _ => throw new RuntimeException(s"Invalid side value: $s")
  }

  def convertProduct(v: JsValue): CoinbaseProductInfo =
    CoinbaseProductInfo(
      symbol = (v \ "product_id").as[String],
      baseIncrement = BigDecimal((v \ "base_increment").as[String]),
      baseMinSize = BigDecimal((v \ "base_min_size").as[String]),
      baseMaxSize = BigDecimal((v \ "base_max_size").as[String]),
      quoteIncrement = BigDecimal((v \ "quote_increment").as[String]),
      quoteMinSize = BigDecimal((v \ "quote_min_size").as[String]),
      quoteMaxSize = BigDecimal((v \ "quote_max_size").as[String]),
    )

  def convertProducts(v: JsValue): Seq[CoinbaseProductInfo] =
    v.as[Seq[JsValue]].map(convertProduct)

}
