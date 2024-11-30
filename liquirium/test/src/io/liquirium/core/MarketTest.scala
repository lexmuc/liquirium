package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.MarketHelpers.{eid, pair}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class MarketTest extends BasicTest {

  test("it provides direct access to base and quote ledger") {
    val m = Market(eid(123), pair("A", "B"))
    m.baseLedger shouldEqual LedgerRef(eid(123), "A")
    m.quoteLedger shouldEqual LedgerRef(eid(123), "B")
  }

}
