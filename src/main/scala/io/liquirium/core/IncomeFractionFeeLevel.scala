package io.liquirium.core

import io.liquirium.core.Trade.Fees

case class IncomeFractionFeeLevel(rate: BigDecimal) extends FeeLevel {

  override def apply(market: Market, amount: BigDecimal, price: BigDecimal): Fees =
    if (amount > BigDecimal(0)) Seq(LedgerRef(market.exchangeId, market.tradingPair.base) -> (amount * rate))
    else Seq(LedgerRef(market.exchangeId, market.tradingPair.quote) -> (-amount * price * rate))

}
