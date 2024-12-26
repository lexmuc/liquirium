package io.liquirium.connect

import io.liquirium.core.Order
import io.liquirium.core.helpers.MarketHelpers
import io.liquirium.core.helpers.async.FutureServiceMock
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, theSameInstanceAs}

class BasicExchangeConnectorTest_LoadOrders extends BasicExchangeConnectorTest {

  private val api =
    new FutureServiceMock[GenericExchangeApi, Set[Order]](_.getOpenOrders(*))

  test("an open order request is simply forwarded") {
    val future = makeConnector(api.instance).loadOpenOrders(MarketHelpers.pair(1))
    api.verify.getOpenOrders(MarketHelpers.pair(1))
    future shouldBe theSameInstanceAs(api.lastReturnedFuture())
  }

}
