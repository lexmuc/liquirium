package io.liquirium.connect.binance

import play.api.libs.json.JsValue

object BinanceApiError {

  def failedJsonConversion(json: JsValue, e: Throwable): BinanceApiError =
    BinanceApiError("Failure when converting json: " + json.toString, exception = Some(e))

}

case class BinanceApiError(message: String, errorCode: Option[Int] = None, exception: Option[Throwable] = None)
  extends Exception(errorCode.map(ec => s"Error code $ec: ").getOrElse("") + message, exception.orNull)

