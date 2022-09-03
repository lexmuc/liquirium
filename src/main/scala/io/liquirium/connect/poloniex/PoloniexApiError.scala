package io.liquirium.connect.poloniex

import play.api.libs.json.JsValue

object PoloniexApiError {
  def apply(message: String, exception: Option[Throwable] = None): OtherPoloniexApiError = OtherPoloniexApiError(message, exception)

  def failedJsonConversion(json: JsValue, e: Throwable): OtherPoloniexApiError =
    PoloniexApiError("Failure when converting json: " + json.toString, exception = Some(e))

  def invalidPricePrecision(price: BigDecimal): OtherPoloniexApiError =
    OtherPoloniexApiError(s"Price has to be rounded to 8 decimal places after the comma: $price", None)

  def invalidQuantityPrecision(price: BigDecimal): OtherPoloniexApiError =
    OtherPoloniexApiError(s"Quantity has to be rounded to 8 decimal places after the comma: $price", None)

}

sealed trait PoloniexApiError extends Exception {
  def message: String
  def exception: Option[Throwable]

  override def getMessage: String = message

  override def getCause: Throwable = exception getOrElse null
}

case class ExplicitPoloniexApiError(code: Int, message: String) extends PoloniexApiError {
  override def exception: Option[Throwable] = None
}

case class OtherPoloniexApiError(message: String, exception: Option[Throwable]) extends PoloniexApiError
