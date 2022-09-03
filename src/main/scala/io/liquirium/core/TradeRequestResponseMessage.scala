package io.liquirium.core

import scala.concurrent.duration.FiniteDuration

object TradeRequestResponseMessage {

  sealed trait SoftFailure extends Exception

  case class NoSuchOpenOrderCancelFailure(orderId: String) extends SoftFailure {
    override def getMessage: String = s"Cancel request failed because order $orderId was not found or not open anymore"
  }

  case object PostOnlyOrderRequestFailed extends SoftFailure {
    override def getMessage: String = "Could not create post-only order"
  }

  case class TradeRequestTimeout(requestId: TradeRequestId, d: FiniteDuration) extends Exception {
    override def getMessage = s"Trade request $requestId failed after ${ d.toSeconds } seconds"
  }

}

sealed trait TradeRequestResponseMessage {
  def requestId: TradeRequestId
}

case class TradeRequestSuccessResponseMessage[TR <: OperationRequest](
  requestId: TradeRequestId,
  response: OperationRequestSuccessResponse[TR],
) extends TradeRequestResponseMessage

final case class TradeRequestFailureMessage(requestId: TradeRequestId, throwable: Throwable)
  extends TradeRequestResponseMessage