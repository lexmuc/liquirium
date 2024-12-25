package io.liquirium.connect

import io.liquirium.bot.helpers.OperationRequestHelpers.operationRequest
import io.liquirium.core.Order
import io.liquirium.core.helpers.{MarketHelpers, TestWithMocks}
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, theSameInstanceAs}

class BasicExchangeConnectorTest_LoadOrders extends AsyncTestWithScheduler with TestWithMocks {

  private val api =
    new FutureServiceMock[GenericExchangeApi, Set[Order]](_.getOpenOrders(*))

  private val connector: BasicExchangeConnector = BasicExchangeConnector.fromExchangeApi(api.instance)

  test("an open order request is simply forwarded") {
    val future = connector.loadOpenOrders(MarketHelpers.pair(1))
    api.verify.getOpenOrders(MarketHelpers.pair(1))
    future shouldBe theSameInstanceAs(api.lastReturnedFuture())
  }

}
