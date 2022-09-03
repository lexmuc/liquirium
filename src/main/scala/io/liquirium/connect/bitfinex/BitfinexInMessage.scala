package io.liquirium.connect.bitfinex

import play.api.libs.json.JsValue

sealed trait BitfinexInMessage

object BitfinexInMessage {

  case class AuthConfirmation() extends BitfinexInMessage

  case class AuthFailure() extends BitfinexInMessage

  case class ErrorMessage(msg: String, code: Int) extends BitfinexInMessage

  case class OrderStateMessage(orders: Seq[BitfinexOrder]) extends BitfinexInMessage

  case class NewTradeMessage(trade: BitfinexTrade) extends BitfinexInMessage

  case class OrderCancelMessage(order: BitfinexOrder) extends BitfinexInMessage

  case class OrderUpdateMessage(order: BitfinexOrder) extends BitfinexInMessage

  case class CancelFailureMessage(orderId: Long) extends BitfinexInMessage

  case class NewOrderMessage(order: BitfinexOrder) extends BitfinexInMessage

  case class SuccessfulOrderRequestNotification(order: BitfinexOrder) extends BitfinexInMessage

  case class HeartbeatMessage(channelId: Long) extends BitfinexInMessage

  case class OrderRequestFailureMessage(clientOrderId: Long, message: String, symbol: String) extends BitfinexInMessage

  case class TickerSubscribedMessage(channelId: Long, pair: String) extends BitfinexInMessage

  case class GeneralChannelMessage(channelId: Long, payload: JsValue) extends BitfinexInMessage {
    def asTickerMessage: TickerMessage = TickerMessage(payload.as[Seq[JsValue]].apply(6).as[BigDecimal])
  }

  case class IrrelevantMessage(json: JsValue) extends BitfinexInMessage

  case class TickerMessage(price: BigDecimal)

}






