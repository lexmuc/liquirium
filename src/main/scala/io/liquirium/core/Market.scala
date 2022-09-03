package io.liquirium.core

case class Market(exchangeId: ExchangeId, tradingPair: TradingPair) {

  val baseLedger: LedgerRef = LedgerRef(exchangeId, tradingPair.base)
  val quoteLedger: LedgerRef = LedgerRef(exchangeId, tradingPair.quote)

}
