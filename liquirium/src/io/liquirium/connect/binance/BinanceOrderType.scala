package io.liquirium.connect.binance

sealed trait BinanceOrderType

object BinanceOrderType {

  case object LIMIT extends BinanceOrderType
  case object MARKET extends BinanceOrderType
  case object STOP_LOSS extends BinanceOrderType
  case object STOP_LOSS_LIMIT extends BinanceOrderType
  case object TAKE_PROFIT extends BinanceOrderType
  case object TAKE_PROFIT_LIMIT extends BinanceOrderType
  case object LIMIT_MAKER extends BinanceOrderType

}

