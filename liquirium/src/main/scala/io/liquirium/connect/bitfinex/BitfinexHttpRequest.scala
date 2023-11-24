package io.liquirium.connect.bitfinex

sealed trait BitfinexHttpRequest {

  def lastPathSegment: String

  def params: Seq[(String, String)]

}

case class PublicBitfinexGetRequest(lastPathSegment: String, params: Seq[(String, String)]) extends BitfinexHttpRequest

case class PrivateBitfinexPostRequest(lastPathSegment: String, body: String, params: Seq[(String, String)])
  extends BitfinexHttpRequest
