package io.liquirium.connect.coinbase

import play.api.libs.json.JsValue

object CoinbaseApiError {

  def apply(message: String, exception: Option[Throwable] = None): OtherCoinbaseApiError = OtherCoinbaseApiError(message, exception)

  def failedJsonConversion(json: JsValue, e: Throwable): OtherCoinbaseApiError =
    CoinbaseApiError("Failure when converting json: " + json.toString, exception = Some(e))

}

sealed trait CoinbaseApiError extends Exception {

  def message: String

  def exception: Option[Throwable]

  override def getMessage: String = message

  override def getCause: Throwable = exception.orNull

}

case class ExplicitCoinbaseApiError(code: Int, message: String) extends CoinbaseApiError {

  override def exception: Option[Throwable] = None

}

case class OtherCoinbaseApiError(message: String, exception: Option[Throwable]) extends CoinbaseApiError
