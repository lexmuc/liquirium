package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.BitfinexOrder.OrderType

sealed trait BitfinexOutMessage

object BitfinexOutMessage {

  sealed trait OrderFlag {
    def intValue: Int
  }

  object OrderFlag {

    case object Hidden extends OrderFlag {
      override def intValue: Int = 64
    }

    case object Close extends OrderFlag {
      override def intValue: Int = 512
    }

    case object ReduceOnly extends OrderFlag {
      override def intValue: Int = 1024
    }

    case object PostOnly extends OrderFlag {
      override def intValue: Int = 4096
    }

    case object OCO extends OrderFlag {
      override def intValue: Int = 16384
    }

    case object NoVarRates extends OrderFlag {
      override def intValue: Int = 524288
    }

  }

  case class AuthMessage(nonce: Long, authenticator: BitfinexAuthenticator) extends BitfinexOutMessage

  sealed trait TradeRequestMessage extends BitfinexOutMessage

  case class CancelOrderMessage(orderId: Long) extends TradeRequestMessage

  case class PlaceOrderMessage(clientOrderId: Long,
                               symbol: String,
                               orderType: OrderType,
                               amount: BigDecimal,
                               price: BigDecimal,
                               flags: Set[OrderFlag] = Set()) extends TradeRequestMessage

  case class SubscribeToTickerMessage(symbol: String) extends BitfinexOutMessage

  case class UnsubscribeFromChannelMessage(channelId: Long) extends BitfinexOutMessage

}




