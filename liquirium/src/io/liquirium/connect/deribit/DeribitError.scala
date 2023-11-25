package io.liquirium.connect.deribit

import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.duration.FiniteDuration

sealed trait DeribitError extends Exception {}


object DeribitError {

  trait ExplicitApiError extends DeribitError

  case class AuthenticationError(message: String, data: Option[JsValue]) extends ExplicitApiError {
    def code: Int = 13009
  }

  case object PostOnlyReject extends ExplicitApiError

  case object NotOpenOrder extends ExplicitApiError

  case class OtherApiError(code: Int, message: String, data: Option[JsValue]) extends ExplicitApiError

  case class ErrorParserFailure(input: JsObject, t: Throwable) extends DeribitError {
    override def getMessage: String = "Failed to parse error json: " + input
    override def getCause: Throwable = t
  }

  case object InterruptedRequestError extends DeribitError {
    override def getMessage: String = "Socket was disconnected while waiting for a response"
  }

  case object InterruptedChannelError extends DeribitError {
    override def getMessage: String = "Socket was disconnected while channel was open"
  }

  case class FailedJsonConversion(json: JsValue, throwable: Throwable) extends DeribitError {
    override def getMessage: String = "Failure when converting json: " + json.toString

    override def getCause: Throwable = throwable
  }

  case class NoAccessTokenReceived(timeout: FiniteDuration) extends DeribitError

}
