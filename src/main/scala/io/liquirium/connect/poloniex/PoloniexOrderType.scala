package io.liquirium.connect.poloniex

sealed trait PoloniexOrderType

object PoloniexOrderType {

  case object MARKET extends PoloniexOrderType

  case object LIMIT extends PoloniexOrderType

  case object LIMIT_MAKER extends PoloniexOrderType // for placing post only orders

}
