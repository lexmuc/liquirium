package io.liquirium.connect.poloniex

sealed trait PoloniexTimeInForce

object PoloniexTimeInForce {

  case object GTC extends PoloniexTimeInForce

  case object IOC extends PoloniexTimeInForce

  case object FOK extends PoloniexTimeInForce

}
