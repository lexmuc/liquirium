package io.liquirium.connect.binance

sealed trait  BinanceHttpRequest {
  def params: Seq[(String, String)]
}

object BinanceHttpRequest {

  case class PublicGet(endpoint: String, params: Seq[(String, String)]) extends BinanceHttpRequest

  case class SignedGet(endpoint: String, params: Seq[(String, String)]) extends BinanceHttpRequest

  case class SignedPost(endpoint: String, params: Seq[(String, String)]) extends BinanceHttpRequest

  case class PostWithApiKey(endpoint: String, params: Seq[(String, String)]) extends BinanceHttpRequest

  case class SignedDelete(endpoint: String, params: Seq[(String, String)]) extends BinanceHttpRequest

}
