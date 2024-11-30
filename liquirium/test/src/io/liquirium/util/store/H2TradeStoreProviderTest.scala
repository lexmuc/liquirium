package io.liquirium.util.store

import io.liquirium.core.Market
import io.liquirium.core.helpers.MarketHelpers.{eid, market}
import io.liquirium.core.helpers.TestWithMocks
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper


class H2TradeStoreProviderTest extends TestWithMocks {

  val storeProvider: (String, Market) => TradeStore = mock(classOf[(String, Market) => TradeStore])

  val store = new H2TradeStoreProvider(storeProvider)

  test("a store is obtained from the given provider with an id constructed from the market") {
    val m = market(eid("EID1"), "BASE1", "QUOTE1")
    val storeA = mock(classOf[TradeStore])
    storeProvider.apply("EID1-BASE1-QUOTE1", m) returns storeA
    store.getStore(m) shouldEqual storeA
  }

}
