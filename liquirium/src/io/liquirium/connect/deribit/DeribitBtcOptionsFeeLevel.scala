package io.liquirium.connect.deribit

import io.liquirium.core.Trade.Fees
import io.liquirium.core.{FeeLevel, Market}

case class DeribitBtcOptionsFeeLevel(
  feePerContract: BigDecimal,
  maxFeeRelativeToPrice: BigDecimal,
) extends FeeLevel {

  override def apply(market: Market, quantity: BigDecimal, price: BigDecimal): Fees = {
    val maxFee = maxFeeRelativeToPrice * price * quantity.abs
    val contractBasedFee = quantity.abs * feePerContract
    val fee = if (contractBasedFee > maxFee) maxFee else contractBasedFee
    Seq(market.quoteLedger -> fee)
  }

}
