package io.liquirium.connect.deribit

sealed trait DeribitDirection

object DeribitDirection {

  case object Buy extends DeribitDirection

  case object Sell extends DeribitDirection

}
