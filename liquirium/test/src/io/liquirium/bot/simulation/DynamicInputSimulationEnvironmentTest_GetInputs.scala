package io.liquirium.bot.simulation

import io.liquirium.eval.Input
import io.liquirium.eval.helpers.ContextHelpers.{FakeContextWithInputs, fakeContextWithInputs, inputUpdate}
import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class DynamicInputSimulationEnvironmentTest_GetInputs extends DynamicInputSimulationEnvironmentTest {

  protected def fakeNextInputUpdate(updates: (Input[_], Any)*): Unit = {
    firstInputUpdateStream.currentInputUpdate returns Some(inputUpdate(updates: _*))
  }

  test("it requests inputs from the stream, then it returns the updated context") {
    val initialContext = fakeContextWithInputs(input(1) -> 1, input(2) -> 2)
    val initialInputRequest = inputRequest(input(3))

    fakeInputStreamTransition(input(3))(
      input(1) -> 11,
      input(3) -> 33,
    )

    val (newContext, _) = environment.getInputs(initialInputRequest, initialContext)
    newContext.asInstanceOf[FakeContextWithInputs].inputValues shouldEqual Map(
      input(1) -> 11,
      input(2) -> 2,
      input(3) -> 33,
    )
  }

  test("the input stream is updated and marketplaces are not changed") {
    val initialContext = fakeContextWithInputs(input(1) -> 1, input(2) -> 2)
    val initialInputRequest = inputRequest(input(3))

    fakeInputStreamTransition(input(3))(
      input(1) -> 11,
      input(3) -> 33,
    )

    val (_, newEnvironment) = environment.getInputs(initialInputRequest, initialContext)
    newEnvironment.inputUpdateStream shouldBe latestFakeInputStream
    newEnvironment.marketplaces shouldBe firstMarketplaces
  }

}
