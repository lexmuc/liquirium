package io.liquirium.core

import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.{BasicTest, MarketHelpers}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class IncomeFractionFeeLevelTest extends BasicTest {

  val eid: ExchangeId = MarketHelpers.eid(123)
  val market: Market = MarketHelpers.market(eid, base = "B", quote = "Q")

  test("for buys the fee is a fraction of the received quantity") {
    IncomeFractionFeeLevel(dec("0.2")).apply(market, dec("2"), price = dec("5")) shouldEqual Seq(
      LedgerRef(eid, "B") -> dec("0.4")
    )
  }

  test("for sells the fee is a fraction of the volume") {
    IncomeFractionFeeLevel(dec("0.2")).apply(market, dec("-2"), price = dec("5")) shouldEqual Seq(
      LedgerRef(eid, "Q") -> dec("2")
    )
  }

}
