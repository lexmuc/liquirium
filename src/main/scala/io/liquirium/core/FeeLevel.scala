package io.liquirium.core

import io.liquirium.core.Trade.Fees

trait FeeLevel {

  def apply(market: Market, quantity: BigDecimal, price: BigDecimal): Fees

}
