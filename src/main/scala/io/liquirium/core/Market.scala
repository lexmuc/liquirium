package io.liquirium.core

case class Market(exchangeId: ExchangeId, tradingPair: TradingPair) {

  val baseLedger: LedgerRef = LedgerRef(exchangeId, tradingPair.base)
  val quoteLedger: LedgerRef = LedgerRef(exchangeId, tradingPair.quote)

  // #TODO MARKET - remove and use pair directly where these fields are accessed
  def base: String = tradingPair.base

  def quote: String = tradingPair.quote

}
