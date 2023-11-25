package io.liquirium.connect.binance

sealed trait BinanceExecutionType

object BinanceExecutionType {
  case object NEW extends BinanceExecutionType
  case object CANCELED extends BinanceExecutionType
  case object REPLACED extends BinanceExecutionType
  case object REJECTED extends BinanceExecutionType
  case object TRADE extends BinanceExecutionType
  case object EXPIRED extends BinanceExecutionType
}
