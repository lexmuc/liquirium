package io.liquirium.util.store

import io.liquirium.core.helpers.CoreHelpers.secs
import io.liquirium.core.helpers.MarketHelpers.{eid, market}
import io.liquirium.core.helpers.TestWithMocks

import java.time.Duration

class H2CandleStoreProviderTest extends TestWithMocks {

  val storeProvider: (String, Duration) => CandleStore = mock[(String, Duration) => CandleStore]

  val store = new H2CandleStoreProvider(storeProvider)

  test("a store is obtained from the given provider with an id constructed from market and resolution") {
    val storeA = mock[CandleStore]
    storeProvider.apply("EID1-BASE1-QUOTE1-123", secs(123)) returns storeA
    store.getStore(market(eid("EID1"), "BASE1", "QUOTE1"), secs(123)) shouldEqual storeA
  }

}
