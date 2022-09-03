package io.liquirium.connect.bitfinex

import play.api.libs.json.JsValue

object BitfinexApiError {

  def apply(message: String, exception: Option[Throwable] = None): OtherBitfinexApiError = OtherBitfinexApiError(message, exception)

  def failedJsonConversion(json: JsValue, e: Throwable): OtherBitfinexApiError =
    BitfinexApiError("Failure when converting json: " + json.toString, exception = Some(e))

}

sealed trait BitfinexApiError extends Exception {

  def message: String

  def exception: Option[Throwable]

  override def getMessage: String = message

  override def getCause: Throwable = exception.orNull

}

case class ExplicitBitfinexApiError(message: String, errorCode: Option[Int] = None) extends BitfinexApiError {

  override def exception: Option[Throwable] = None

}

case class OtherBitfinexApiError(message: String, exception: Option[Throwable]) extends BitfinexApiError