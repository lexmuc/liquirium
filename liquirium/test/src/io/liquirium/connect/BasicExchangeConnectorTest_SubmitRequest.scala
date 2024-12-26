package io.liquirium.connect

import io.liquirium.bot.helpers.OperationRequestHelpers.operationRequest
import io.liquirium.core.OperationRequestSuccessResponse
import io.liquirium.core.helpers.async.FutureServiceMock
import org.scalatest.matchers.should.Matchers._

class BasicExchangeConnectorTest_SubmitRequest extends BasicExchangeConnectorTest {

  private val api =
    new FutureServiceMock[GenericExchangeApi, OperationRequestSuccessResponse[_]](_.sendTradeRequest(*))

  test("a trade operation request is simply forwarded") {
    val future = makeConnector(api.instance).submitRequest(operationRequest(1))
    api.verify.sendTradeRequest(operationRequest(1))
    future shouldBe theSameInstanceAs(api.lastReturnedFuture())
  }

}
