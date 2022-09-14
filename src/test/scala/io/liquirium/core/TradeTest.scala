package io.liquirium.core

import io.liquirium.core.Transaction.Effect
import io.liquirium.core.helper.MarketHelpers.{eid, market}
import io.liquirium.core.helper.TradeHelpers.trade
import io.liquirium.core.helper.BasicTest
import io.liquirium.core.helper.CoreHelpers.{dec, sec}

class TradeTest extends BasicTest {

  test("the history id is simply the id") {
    trade(id = "asdf").historyId shouldEqual "asdf"
  }

  test("the history timestamp is simply the timestamp") {
    trade(time = sec(777)).historyTimestamp shouldEqual sec(777)
  }

  test("a buy reduces the quote currency by the volume and increases the base currency by the amount") {
    trade(market = market(eid(123), "B", "Q"), quantity = dec("2.0"), price = dec("1.25"), fees = Seq())
      .effects should contain theSameElementsAs Seq(
      Effect(LedgerRef(eid(123), "B"), dec("2.0")),
      Effect(LedgerRef(eid(123), "Q"), dec("-2.5"))
    )
  }

  test("a sell increases the quote currency by the volume and decreases the base currency by the amount") {
    trade(market = market(eid(123), "B", "Q"), quantity = dec("-2.0"), price = dec("1.25"), fees = Seq())
      .effects should contain theSameElementsAs Seq(
      Effect(LedgerRef(eid(123), "B"), dec("-2.0")),
      Effect(LedgerRef(eid(123), "Q"), dec("2.5"))
    )
  }

  test("additional fees (positive fee => negative effect) are included in and combined with the effects") {
    trade(market = market(eid(123), "B", "Q"), quantity = dec("-2.0"), price = dec("1.25"), fees = Seq(
      LedgerRef(eid(123), "B") -> dec("0.1"),
      LedgerRef(eid(123), "Q") -> dec("-0.1"),
      LedgerRef(eid(234), "X") -> dec("0.2")
    )).effects should contain theSameElementsAs Seq(
      Effect(LedgerRef(eid(123), "B"), dec("-2.1")),
      Effect(LedgerRef(eid(123), "Q"), dec("2.6")),
      Effect(LedgerRef(eid(234), "X"), dec("-0.2"))
    )
  }

  test("the volume is the product of (absolute) quantity and price") {
    trade(quantity = dec("2.5"), price = dec("1.5")).volume shouldEqual dec("3.75")
    trade(quantity = dec("-2.5"), price = dec("1.5")).volume shouldEqual dec("3.75")
  }

  test("trades with positive amount are buys, not sells") {
    trade(quantity = dec("2")).isBuy shouldBe true
    trade(quantity = dec("2")).isSell shouldBe false
  }

  test("trades with negative amount are sells, not buys") {
    trade(quantity = dec("-2")).isBuy shouldBe false
    trade(quantity = dec("-2")).isSell shouldBe true
  }

  test("trades are compared by timestamp or by id if timestamp is equal") {
    trade(sec(1), "B") shouldBe < (trade(sec(2), "A"))(Ordering.fromLessThan(_ < _))
    trade(sec(1), "B") shouldBe > (trade(sec(1), "A"))(Ordering.fromLessThan(_ < _))
  }

}
