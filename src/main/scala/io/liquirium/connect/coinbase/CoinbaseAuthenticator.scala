package io.liquirium.connect.coinbase

import io.liquirium.util.{ApiCredentials, Clock, HmacCalculator}

class CoinbaseAuthenticator(private val credentials: ApiCredentials, private val clock: Clock) {

  def getHeadersForGetRequest(path: String): Map[String, String] = {
    val timestamp = clock.getTime.getEpochSecond.toString
    headers(
      timestamp = timestamp,
      dataToSign = timestamp + "GET" + path
    )
  }

  def getHeadersForPostRequest(path: String, body: String): Map[String, String] = {
    val timestamp = clock.getTime.getEpochSecond.toString
    headers(
      timestamp = timestamp,
      dataToSign = timestamp + "POST" + path + body
    )
  }

  private def headers(timestamp: String, dataToSign: String): Map[String, String] =
    Map(
      "accept" -> "application/json",
      "CB-ACCESS-KEY" -> credentials.apiKey,
      "CB-ACCESS-SIGN" -> HmacCalculator.sha256(dataToSign, credentials.secret),
      "CB-ACCESS-TIMESTAMP" -> timestamp,
    )

}