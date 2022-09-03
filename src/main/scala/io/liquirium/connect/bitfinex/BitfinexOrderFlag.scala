package io.liquirium.connect.bitfinex

sealed trait BitfinexOrderFlag

object BitfinexOrderFlag {

  case object Hidden extends BitfinexOrderFlag

  case object Close extends BitfinexOrderFlag

  case object ReduceOnly extends BitfinexOrderFlag

  case object PostOnly extends BitfinexOrderFlag

  case object OCO extends BitfinexOrderFlag

  case object NoVarRates extends BitfinexOrderFlag

}
