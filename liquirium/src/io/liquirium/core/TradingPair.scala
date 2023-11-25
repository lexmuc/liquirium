package io.liquirium.core

case class TradingPair(base: String, quote: String) {
  def flip: TradingPair = copy(quote, base)
}
