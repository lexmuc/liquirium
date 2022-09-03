package io.liquirium.util

sealed trait ResultOrder

object ResultOrder {

  case object AscendingOrder extends ResultOrder

  case object DescendingOrder extends ResultOrder

}
