package io.liquirium.connect

import io.liquirium.bot.helpers.OperationRequestHelpers.operationRequest
import io.liquirium.core.OperationRequestSuccessResponse
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import org.scalatest.matchers.should.Matchers._

class BasicExchangeConnectorTest_SubmitRequest extends AsyncTestWithScheduler with TestWithMocks {

  private val api =
    new FutureServiceMock[GenericExchangeApi, OperationRequestSuccessResponse[_]](_.sendTradeRequest(*))

  private val connector: BasicExchangeConnector = BasicExchangeConnector.fromExchangeApi(api.instance)

  test("a trade operation request is simply forwarded") {
    val future = connector.submitRequest(operationRequest(1))
    api.verify.sendTradeRequest(operationRequest(1))
    future shouldBe theSameInstanceAs(api.lastReturnedFuture())
  }

}
