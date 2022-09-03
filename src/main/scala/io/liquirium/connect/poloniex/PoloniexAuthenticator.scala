package io.liquirium.connect.poloniex

import io.liquirium.util.{ApiCredentials, Clock, HmacCalculator}

import java.net.URLEncoder

class PoloniexAuthenticator(private val credentials: ApiCredentials, private val clock: Clock) {

  def getHeadersForGetRequest(path: String, params: Seq[(String, String)]): Map[String, String] = {
    val timestamp = clock.getTime.toEpochMilli.toString
    val paramQuery = queryString((params :+ ("signTimestamp", timestamp)).sortBy(p => p._1))
    headers(
      timestamp = timestamp,
      dataToSign = "GET" + "\n" + path + "\n" + paramQuery,
    )
  }

  def getHeadersForPostRequest(path: String, body: String): Map[String, String] = {
    val timestamp = clock.getTime.toEpochMilli.toString
    val bodyPart = if (body.nonEmpty) "requestBody=" + body + "&" else ""
    val bodyAndTimestamp = bodyPart + "signTimestamp=" + timestamp
    headers(
      timestamp = timestamp,
      dataToSign = "POST" + "\n" + path + "\n" + bodyAndTimestamp,
    )
  }

  def getHeadersForDeleteRequest(path: String, body: String): Map[String, String] = {
    val timestamp = clock.getTime.toEpochMilli.toString
    val bodyPart = if (body.nonEmpty) "requestBody=" + body + "&" else ""
    val bodyAndTimestamp = bodyPart + "signTimestamp=" + timestamp
    headers(
      timestamp = timestamp,
      dataToSign = "DELETE" + "\n" + path + "\n" + bodyAndTimestamp,
    )
  }

  private def headers(timestamp: String, dataToSign: String): Map[String, String] =
    Map(
      "key" -> credentials.apiKey,
      "signatureMethod" -> "hmacSHA256",
      "signatureVersion" -> "1",
      "signTimestamp" -> timestamp,
      "signature" -> HmacCalculator.sha256Base64(dataToSign, credentials.secret),
    )

  private def queryString(params: Seq[(String, String)]): String = {
    val urlEncode = (s: String) => URLEncoder.encode(s, "UTF-8")
    params.map { case (k, v) => urlEncode(k) + "=" + urlEncode(v) }.mkString("&")
  }


}