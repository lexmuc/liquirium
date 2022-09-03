package io.liquirium.connect.bitfinex

import io.liquirium.util.HmacCalculator

case class BitfinexAuthenticator(val apiKey: String, private val secret: String) {

  def sign(s: String) = HmacCalculator.sha384(s, secret)

}
