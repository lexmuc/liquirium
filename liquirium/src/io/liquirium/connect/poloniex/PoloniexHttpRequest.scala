package io.liquirium.connect.poloniex

sealed trait PoloniexHttpRequest {
  def path: String

  def params: Seq[(String, String)]
}

object PoloniexHttpRequest {

  case class PublicGet(path: String, params: Seq[(String, String)]) extends PoloniexHttpRequest

  case class PrivateGet(path: String, params: Seq[(String, String)]) extends PoloniexHttpRequest

  case class PrivatePost(path: String, params: Seq[(String, String)]) extends PoloniexHttpRequest

  case class PrivateDelete(path: String, params: Seq[(String, String)]) extends PoloniexHttpRequest

}




