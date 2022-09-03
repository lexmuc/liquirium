package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.CoinbaseHttpRequest.{PrivateGet, PrivatePost}
import io.liquirium.core.Side
import io.liquirium.util.akka.AsyncRequest
import play.api.libs.json._

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.{Date, Locale, TimeZone}

sealed trait CoinbaseApiRequest[T] extends AsyncRequest[T] {

  def httpRequest: CoinbaseHttpRequest

  def convertResponse(json: JsValue, converter: CoinbaseJsonConverter): T

  def convertFailure(e: Throwable): Throwable = e

}

object CoinbaseApiRequest {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH)
  dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))

  private def formatDate(i: Instant) = dateFormat.format(Date.from(i))

  case class GetCandles(
    productId: String,
    start: Instant,
    end: Instant,
    granularity: CoinbaseCandleLength,
  ) extends CoinbaseApiRequest[Seq[CoinbaseCandle]] {

    override def httpRequest: PrivateGet = {
      val s = start.toEpochMilli
      val e = end.toEpochMilli
      if (s % 1000 != 0 || e % 1000 != 0) throw new RuntimeException("start and end time must be set in full seconds")

      val params = (Seq("product_id" -> productId)
        ++ Seq("start" -> start.getEpochSecond.toString)
        ++ Seq("end" -> end.getEpochSecond.toString)
        ++ Seq("granularity" -> granularity.code)
        )
      PrivateGet(s"/api/v3/brokerage/products/$productId/candles", params)
    }

    override def convertResponse(json: JsValue, converter: CoinbaseJsonConverter): Seq[CoinbaseCandle] = {
      converter.convertCandles((json \ "candles").get)
    }

  }

  case class GetOpenOrders(
    productId: Option[String],
    limit: Int, // 3000
  ) extends CoinbaseApiRequest[Seq[CoinbaseOrder]] {

    override def httpRequest: PrivateGet = {
      val params = (Seq(
        // "order_status" -> "PENDING", // "error_details":"failed to parse Query request does not support querying active status orders"
        "limit" -> limit.toString,
      )
        ++ productId.map(x => ("product_id", x))
        )
      PrivateGet("/api/v3/brokerage/orders/historical/batch", params)
    }

    def convertResponse(json: JsValue, converter: CoinbaseJsonConverter): Seq[CoinbaseOrder] = {

      val hasNextPage = (json \ "has_next").as[Boolean]
      if (hasNextPage) throw new RuntimeException("Pagination in order response found - not yet supported")

      converter.convertOrders((json \ "orders").get)
    }

  }

  case class CreateOrder(
    side: Side,
    productId: String,
    clientOrderId: String, //Client set unique uuid for this order
    baseSize: BigDecimal,
    limitPrice: BigDecimal,
    postOnly: Boolean,
  ) extends CoinbaseApiRequest[CoinbaseCreateOrderResponse] {

    override def httpRequest: PrivatePost = {

      val orderConfiguration = JsObject(Seq(
        "limit_limit_gtc" -> JsObject(Seq(
          "base_size" -> JsString(s"$baseSize"),
          "limit_price" -> JsString(s"$limitPrice"),
          "post_only" -> JsBoolean(postOnly),
        ))
      ))
      val body = JsObject(Seq(
        ("side", JsString(if (side == Side.Buy) "BUY" else "SELL")),
        ("product_id", JsString(productId)),
        ("client_order_id", JsString(clientOrderId)),
        ("order_configuration", orderConfiguration),
      ))
      CoinbaseHttpRequest.PrivatePost("/api/v3/brokerage/orders", body)
    }

    override def convertResponse(json: JsValue, converter: CoinbaseJsonConverter): CoinbaseCreateOrderResponse = {
      converter.convertCreateOrderResponse(json)
    }
  }

  case class CancelOrders(
    orderIds: Seq[String],
  ) extends CoinbaseApiRequest[Seq[CoinbaseCancelOrderResult]] {

    override def httpRequest: PrivatePost = {
      val body = JsObject(Seq(
        ("order_ids", JsArray(orderIds.map(JsString).toIndexedSeq)),
      ))
      CoinbaseHttpRequest.PrivatePost("/api/v3/brokerage/orders/batch_cancel", body)
    }

    override def convertResponse(json: JsValue, converter: CoinbaseJsonConverter): Seq[CoinbaseCancelOrderResult] = {
      converter.convertCancelOrderResults((json \ "results").get)
    }

  }

  case class GetTradeHistory(
    orderId: Option[String],
    productId: Option[String],
    startSequenceTimestamp: Option[Instant],
    endSequenceTimestamp: Option[Instant],
    limit: Option[Long],
  ) extends CoinbaseApiRequest[Seq[CoinbaseTrade]] {

    def httpRequest: PrivateGet = {
      val params = (Seq()
        ++ orderId.map(x => ("order_id", x))
        ++ productId.map(x => ("product_id", x))
        ++ startSequenceTimestamp.map(x => ("start_sequence_timestamp", formatDate(x)))
        ++ endSequenceTimestamp.map(x => ("end_sequence_timestamp", formatDate(x)))
        ++ limit.map(x => ("limit", x.toString))
        )
      PrivateGet("/api/v3/brokerage/orders/historical/fills", params)
    }

    def convertResponse(json: JsValue, converter: CoinbaseJsonConverter): Seq[CoinbaseTrade] = {
      if ((json \ "cursor").as[String] != "") throw new RuntimeException("cursor in trade response found - not yet supported")
      converter.convertTrades((json \ "fills").get)
    }

  }

  case class ListProducts() extends CoinbaseApiRequest[Seq[CoinbaseProductInfo]] {
    override def httpRequest: CoinbaseHttpRequest = {
      val path = "/api/v3/brokerage/products"
      PrivateGet(path, Seq())
    }

    override def convertResponse(json: JsValue, converter: CoinbaseJsonConverter): Seq[CoinbaseProductInfo] = {
      val products = json("products")
      converter.convertProducts(products)
    }
  }

}