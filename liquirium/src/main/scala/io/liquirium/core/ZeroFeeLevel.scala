package io.liquirium.core

import io.liquirium.core.Trade.Fees

object ZeroFeeLevel extends FeeLevel {

  override def apply(market: Market, amount: BigDecimal, price: BigDecimal): Fees = Seq()

}
