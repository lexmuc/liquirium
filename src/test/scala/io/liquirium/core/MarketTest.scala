package io.liquirium.core

import io.liquirium.core.helper.BasicTest
import io.liquirium.core.helper.MarketHelpers.{eid, pair}

class MarketTest extends BasicTest {

  test("it provides direct access to base and quote ledger") {
    val m = Market(eid(123), pair("A", "B"))
    m.baseLedger shouldEqual LedgerRef(eid(123), "A")
    m.quoteLedger shouldEqual LedgerRef(eid(123), "B")
  }

}
