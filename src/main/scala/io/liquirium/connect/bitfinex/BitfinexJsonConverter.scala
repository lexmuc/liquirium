package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex
import io.liquirium.connect.bitfinex.BitfinexOrder.{OrderStatus, OrderType}
import io.liquirium.connect.bitfinex.BitfinexOutMessage._
import play.api.libs.json._

import java.time.Instant

class BitfinexJsonConverter() {

  def convertSingleOrder(json: JsValue): BitfinexOrder = {
    val arr = json.as[Seq[JsValue]]
    bitfinex.BitfinexOrder(
      id = arr(0).as[Long],
      clientOrderId = arr(2).as[Long],
      symbol = arr(3).as[String],
      creationTimestamp = Instant.ofEpochMilli(arr(4).as[Long]),
      updateTimestamp = Instant.ofEpochMilli(arr(5).as[Long]),
      amount = arr(6).as[BigDecimal],
      originalAmount = arr(7).as[BigDecimal],
      `type` = parseOrderType(arr(8).as[String]),
      status = parseOrderStatus(arr(13).as[String]),
      price = arr(16).as[BigDecimal]
    )
  }

  private def parseOrderType(s: String): OrderType = s match {
    case "LIMIT" => OrderType.Limit
    case "EXCHANGE LIMIT" => OrderType.ExchangeLimit
    case "MARKET" => OrderType.Market
    case "EXCHANGE MARKET" => OrderType.ExchangeMarket
    case _ => throw new RuntimeException("Unsupported order type: " + s)
  }

  private def parseOrderStatus(s: String): OrderStatus = s match {
    case "ACTIVE" => OrderStatus.Active
    case "CANCELED" => OrderStatus.Canceled
    case "POSTONLY CANCELED" => OrderStatus.PostOnlyCanceled
    case x if x.startsWith("PARTIALLY FILLED @") => OrderStatus.PartiallyFilled
    case x if x.startsWith("CANCELED ") => OrderStatus.Canceled
    case x if x.startsWith("EXECUTED @") => OrderStatus.Executed
    case _ => throw new RuntimeException("Unsupported order status: " + s)
  }

  def convertOrderType(ot: OrderType): JsValue = ot match {
    case OrderType.Limit => JsString("LIMIT")
    case OrderType.Market => JsString("MARKET")
    case OrderType.ExchangeLimit => JsString("EXCHANGE LIMIT")
    case OrderType.ExchangeMarket => JsString("EXCHANGE MARKET")
    case OrderType.Stop => JsString("STOP")
    case OrderType.ExchangeStop => JsString("EXCHANGE STOP")
    case OrderType.StopLimit => JsString("STOP LIMIT")
    case OrderType.ExchangeStopLimit => JsString("EXCHANGE STOP LIMIT")
    case OrderType.TrailingStop => JsString("TRAILING STOP")
    case OrderType.ExchangeTrailingStop => JsString("EXCHANGE TRAILING STOP")
    case OrderType.Fok => JsString("FOK")
    case OrderType.ExchangeFok => JsString("EXCHANGE FOK")
    case OrderType.Ioc => JsString("IOC")
    case OrderType.ExchangeIoc => JsString("EXCHANGE IOC")
  }

  def convertOrders(v: JsValue): Seq[BitfinexOrder] = v.as[Seq[JsValue]].map(convertSingleOrder)

  def convertSingleTrade(json: JsValue): BitfinexTrade = {
    val arr = json.as[Seq[JsValue]]
    bitfinex.BitfinexTrade(
      id = arr(0).as[Long],
      symbol = arr(1).as[String],
      timestamp = Instant.ofEpochMilli(arr(2).as[Long]),
      orderId = arr(3).as[Long],
      amount = arr(4).as[BigDecimal],
      price = arr(5).as[BigDecimal],
      fee = arr(9).as[BigDecimal],
      feeCurrency = arr(10).as[String]
    )
  }

  def convertTrades(v: JsValue): Seq[BitfinexTrade] = v.as[Seq[JsValue]].map(convertSingleTrade)

  def convertSingleCandle(v: JsValue): BitfinexCandle = BitfinexCandle(
    timestamp = Instant.ofEpochMilli((v \ 0).get.as[Long]),
    open = (v \ 1).get.as[BigDecimal],
    close = (v \ 2).get.as[BigDecimal],
    high = (v \ 3).get.as[BigDecimal],
    low = (v \ 4).get.as[BigDecimal],
    volume = (v \ 5).get.as[BigDecimal]
  )

  def convertCandles(v: JsValue): Seq[BitfinexCandle] = v.as[Seq[JsValue]].map(convertSingleCandle)

  def convertOutgoingMessage(m: BitfinexOutMessage): JsValue = m match {

    case AuthMessage(nonce, authenticator) =>
      JsObject(Seq(
        "event" -> JsString("auth"),
        "apiKey" -> JsString(authenticator.apiKey),
        "authNonce" -> JsNumber(nonce),
        "authPayload" -> JsString(s"AUTH$nonce"),
        "authSig" -> JsString(authenticator.sign(s"AUTH$nonce"))
      ))

    case CancelOrderMessage(id) =>
      JsArray(Seq(JsNumber(0), JsString("oc"), JsNull,
        JsObject(Seq("id" -> JsNumber(id)))
      ))

    case PlaceOrderMessage(cid, symbol, orderType, amount, price, flags) =>
      JsArray(Seq(JsNumber(0), JsString("on"), JsNull,
        JsObject(Seq(
          "cid" -> JsNumber(cid),
          "type" -> convertOrderType(orderType),
          "symbol" -> JsString(symbol),
          "amount" -> JsString(amount.bigDecimal.toPlainString),
          "price" -> JsString(price.bigDecimal.toPlainString),
          "flags" -> JsNumber(flags.foldLeft(0)(_ | _.intValue))
        ))
      ))

    case SubscribeToTickerMessage(symbol) =>
      JsObject(Seq(
        "event" -> JsString("subscribe"),
        "channel" -> JsString("ticker"),
        "symbol" -> JsString(symbol)
      ))

    case UnsubscribeFromChannelMessage(channelId) =>
      JsObject(Seq(
        "event" -> JsString("unsubscribe"),
        "chanId" -> JsNumber(channelId)
      ))

  }

  def convertPairInfos(v: JsValue): Seq[BitfinexPairInfo] =
    v.as[Seq[JsValue]].map(convertPairInfo)

  def convertPairInfo(v: JsValue): BitfinexPairInfo = {
    val subArray = v(1).as[Seq[JsValue]]
    BitfinexPairInfo(
      pair = v(0).as[String],
      minOrderSize = BigDecimal(subArray(3).as[String]),
      maxOrderSize = BigDecimal(subArray(4).as[String]),
    )
  }

}
