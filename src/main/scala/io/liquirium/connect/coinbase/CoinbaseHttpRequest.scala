package io.liquirium.connect.coinbase

import play.api.libs.json.JsValue

sealed trait CoinbaseHttpRequest {

  def path: String

}

object CoinbaseHttpRequest {

  case class PrivateGet(path: String, params: Seq[(String, String)]) extends CoinbaseHttpRequest

  case class PrivatePost(path: String, body: JsValue) extends CoinbaseHttpRequest

}




