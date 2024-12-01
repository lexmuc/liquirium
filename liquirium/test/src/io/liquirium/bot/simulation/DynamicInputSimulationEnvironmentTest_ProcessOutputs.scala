package io.liquirium.bot.simulation

import io.liquirium.bot.helpers.BotOutputHelpers.logOutput
import io.liquirium.bot.helpers.OperationRequestHelpers.operationRequestMessage
import io.liquirium.eval.helpers.ContextHelpers.{context, fakeContextWithInputs, inputUpdate}
import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest}
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class DynamicInputSimulationEnvironmentTest_ProcessOutputs extends DynamicInputSimulationEnvironmentTest {

  test("when empty outputs are processed, the same context and environment is returned") {
    val (newContext, newEnvironment) = environment.processOutputs(Seq(), context(1))
    newContext shouldEqual context(1)
    newEnvironment shouldEqual environment
  }

  test("trade requests messages are forwarded to the marketplaces one by one and the final context is returned") {
    val mp1 = firstMarketplaces
    val mp2 = mock(classOf[SimulationMarketplaces])
    val mp3 = mock(classOf[SimulationMarketplaces])
    mp1.processOperationRequest(operationRequestMessage(1), context(1)) returns Right((context(2), mp2))
    mp2.processOperationRequest(operationRequestMessage(2), context(2)) returns Right((context(3), mp3))
    val outputs = Seq(operationRequestMessage(1), operationRequestMessage(2))
    val (newContext, newEnvironment) = environment.processOutputs(outputs, context(1))
    newContext shouldEqual context(3)
    newEnvironment shouldEqual environment.copy(marketplaces = mp3)
  }

  test("the marketplaces are properly updated") {
    val mp1 = firstMarketplaces
    val mp2 = mock(classOf[SimulationMarketplaces])
    mp1.processOperationRequest(operationRequestMessage(1), context(1)) returns Right((context(2), mp2))
    val outputs = Seq(operationRequestMessage(1))
    val (_, newEnvironment) = environment.processOutputs(outputs, context(1))
    newEnvironment shouldEqual environment.copy(marketplaces = mp2)
  }

  test("upon an input request processing is repeated with an updated context until the request can be processed") {
    val initContext = fakeContextWithInputs(input(0) -> 0)
    val mp1 = firstMarketplaces
    val mp2 = mock(classOf[SimulationMarketplaces])
    mp1.processOperationRequest(operationRequestMessage(1), initContext) returns Left(inputRequest(input(1)))

    fakeInputStreamTransition(input(1))(input(1) -> 11)
    val contextAfterFirstUpdate = initContext.update(inputUpdate(input(1) -> 11))

    mp1.processOperationRequest(operationRequestMessage(1), contextAfterFirstUpdate) returns Left(inputRequest(input(2)))
    fakeInputStreamTransition(input(2))(input(1) -> 11, input(2) -> 22)
    val contextAfterSecondUpdate = contextAfterFirstUpdate.update(inputUpdate(input(1) -> 11, input(2) -> 22))

    mp1.processOperationRequest(operationRequestMessage(1), contextAfterSecondUpdate) returns Right(context(2), mp2)

    val outputs = Seq(operationRequestMessage(1))
    val (newContext, newEnvironment) = environment.processOutputs(outputs, initContext)
    newContext shouldEqual context(2)
    newEnvironment.marketplaces shouldEqual mp2
    newEnvironment.inputUpdateStream shouldEqual latestFakeInputStream
  }

  test("outputs other than operation request messages are ignored") {
    val mp1 = firstMarketplaces
    val mp2 = mock(classOf[SimulationMarketplaces])
    mp1.processOperationRequest(operationRequestMessage(1), context(1)) returns Right((context(2), mp2))
    val outputs = Seq(logOutput(1), operationRequestMessage(1), logOutput(2))
    val (newContext, newEnvironment) = environment.processOutputs(outputs, context(1))
    newEnvironment shouldEqual environment.copy(marketplaces = mp2)
    newContext shouldEqual context(2)
  }

}
