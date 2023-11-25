package io.liquirium.connect.binance

import io.liquirium.util.HmacCalculator

case class BinanceAuthenticator(apiKey: String, private val secret: String) {

  def sign(s: String): String = HmacCalculator.sha256(s, secret)

}
