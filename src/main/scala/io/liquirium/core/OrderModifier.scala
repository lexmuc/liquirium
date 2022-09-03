package io.liquirium.core

sealed trait OrderModifier

object OrderModifier {

  case object Margin extends OrderModifier

  case object FillOrKill extends OrderModifier

  case object PostOnly extends OrderModifier

  case object AutoMovePostOnly extends OrderModifier

  case object MarketOrder extends OrderModifier

}
