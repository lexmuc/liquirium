package io.liquirium.connect.deribit

import io.liquirium.core.LedgerRef
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.{BasicTest, MarketHelpers}

class DeribitBtcOptionsFeeLevelTest extends BasicTest {

  var feePerContract: BigDecimal = dec("0.0")
  var maxFeeRelativeToPrice: BigDecimal = dec("1.0")

  val market = MarketHelpers.market(MarketHelpers.eid(1), "OPT", "BTC")

  def moneyFee(f: BigDecimal) = Seq(LedgerRef(market.exchangeId, market.tradingPair.quote) -> f)

  def apply(price: BigDecimal, quantity: BigDecimal) = DeribitBtcOptionsFeeLevel(
    feePerContract = feePerContract,
    maxFeeRelativeToPrice = maxFeeRelativeToPrice
  )(market, quantity, price)

  test("for normal prices, the fee is a certain fraction of the absolute quantity") {
    feePerContract = dec("0.0005")
    apply(price = dec("0.123"), quantity = dec("2")) shouldEqual moneyFee(dec("0.001"))
    apply(price = dec("0.123"), quantity = dec("-2")) shouldEqual moneyFee(dec("0.001"))
  }

  test("for very low prices, the fee is a fraction of the price") {
    feePerContract = dec("0.0005")
    maxFeeRelativeToPrice = dec("0.1")
    apply(price = dec("0.0001"), quantity = dec("2")) shouldEqual moneyFee(dec("0.00002"))
    apply(price = dec("0.0001"), quantity = dec("-2")) shouldEqual moneyFee(dec("0.00002"))
  }

}
