package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexInMessage._
import play.api.libs.json._


class BitfinexMessageConverter(jsonConverter: BitfinexJsonConverter) {

  val zero = JsNumber(BigDecimal("0"))

  def convertIncomingMessage(j: JsValue): BitfinexInMessage =
    j match {
      case obj: JsObject =>
        (obj \ "event").asOpt[String] match {
          case Some("auth") => if (obj("status").as[String] == "OK") AuthConfirmation() else AuthFailure()
          case Some("error") => ErrorMessage(obj("msg").as[String], obj("code").as[Int])
          case Some("subscribed") if (obj \ "channel").as[String] == "ticker" =>
            TickerSubscribedMessage((obj \ "chanId").as[Long], (obj \ "pair").as[String])
          case _ => IrrelevantMessage(j)
        }
      case arr: JsArray =>
        arr.value.toSeq match {
          case Seq(`zero`, JsString("os"), json: JsValue) => OrderStateMessage(jsonConverter.convertOrders(json))
          case Seq(`zero`, JsString("on"), json: JsValue) => NewOrderMessage(jsonConverter.convertSingleOrder(json))
          case Seq(`zero`, JsString("ou"), json: JsValue) => OrderUpdateMessage(jsonConverter.convertSingleOrder(json))
          case Seq(`zero`, JsString("oc"), json: JsValue) => OrderCancelMessage(jsonConverter.convertSingleOrder(json))
          case Seq(`zero`, JsString("tu"), json: JsValue) => NewTradeMessage(jsonConverter.convertSingleTrade(json))
          case Seq(channelId: JsValue, JsString("hb")) => HeartbeatMessage(channelId.as[Long])
          case Seq(`zero`, JsString("n"), notificationJson: JsValue) => convertNotificationJson(notificationJson, j)
          case Seq(channelId: JsValue, payload: JsValue) if channelId != zero => GeneralChannelMessage(channelId.as[Long], payload)
          case _ => IrrelevantMessage(j)
        }
      case _ => IrrelevantMessage(j)
    }

  def convertNotificationJson(j: JsValue, completeJson: JsValue): BitfinexInMessage = j match {
    case arr: JsArray => {
      val v = arr.value
      val notificationType = v(1).as[String]
      val status = v(6).as[String]
      if (notificationType == "on-req" && status == "ERROR") {
        val clientOrderId = v(4).as[Seq[JsValue]].apply(2).as[Long]
        val symbol = v(4).as[Seq[JsValue]].apply(3).as[String]
        val msg = v(7).as[String]
        OrderRequestFailureMessage(clientOrderId, msg, symbol = symbol)
      }
      else if (notificationType == "on-req" && status == "SUCCESS")
        SuccessfulOrderRequestNotification(jsonConverter.convertSingleOrder(v(4)))
      else if (notificationType == "oc-req" && status == "ERROR")
        CancelFailureMessage(v(4).as[Seq[JsValue]].apply(0).as[Long])
      else
        IrrelevantMessage(completeJson)
    }
    case _ => IrrelevantMessage(completeJson)
  }

}