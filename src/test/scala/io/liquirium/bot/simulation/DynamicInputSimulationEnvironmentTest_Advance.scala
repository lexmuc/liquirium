package io.liquirium.bot.simulation

import io.liquirium.eval.helpers.ContextHelpers.{context, fakeContextWithInputs, inputUpdate}
import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest}

class DynamicInputSimulationEnvironmentTest_Advance extends DynamicInputSimulationEnvironmentTest {

  test("advancing advances the input update stream and fills orders with the updated context") {
    val nextInputUpdateStream = mock[SimulationInputUpdateStream]
    nextInputUpdateStream.currentInputUpdate returns Some(inputUpdate(input(2) -> 2, input(3) -> 3))
    firstInputUpdateStream.advance returns nextInputUpdateStream
    val inputContext = fakeContextWithInputs(input(1) -> 1)
    val intermediateContext = fakeContextWithInputs(
      input(1) -> 1,
      input(2) -> 2,
      input(3) -> 3,
    )
    val expectedOutputContext = fakeContextWithInputs(input(4)-> 4)
    fakeMarketplacesOrderFill(intermediateContext, expectedOutputContext)
    val (observedOutputContext, newEnvironment) = environment.advance(inputContext)
    newEnvironment.inputUpdateStream shouldEqual nextInputUpdateStream
    newEnvironment.marketplaces shouldEqual expectedMarketplaces
    observedOutputContext shouldEqual expectedOutputContext
  }

  test("input requests are resolved against the stream until the marketplaces can be advanced") {
    val nextInputUpdateStream = mock[SimulationInputUpdateStream]
    nextInputUpdateStream.currentInputUpdate returns Some(inputUpdate(input(1) -> 11))
    firstInputUpdateStream.advance returns nextInputUpdateStream
    latestFakeInputStream = nextInputUpdateStream

    val initContext = fakeContextWithInputs(input(0) -> 0)
    val contextAfterSteamAdvance = initContext.update(inputUpdate(input(1) -> 11))

    firstMarketplaces.executeActivatedOrders(contextAfterSteamAdvance) returns Left(inputRequest(input(2)))
    fakeInputStreamTransition(input(2))(input(2) -> 22)
    val contextAfterFirstUpdate = contextAfterSteamAdvance.update(inputUpdate(input(2) -> 22))

    firstMarketplaces.executeActivatedOrders(contextAfterFirstUpdate) returns Left(inputRequest(input(3)))
    fakeInputStreamTransition(input(3))(input(3) -> 33)
    val contextAfterSecondUpdate = contextAfterFirstUpdate.update(inputUpdate(input(3) -> 33))

    val finalContext = context(123)
    fakeMarketplacesOrderFill(contextAfterSecondUpdate, finalContext)
    val (observedOutputContext, newEnvironment) = environment.advance(initContext)
    newEnvironment.inputUpdateStream shouldEqual latestFakeInputStream
    newEnvironment.marketplaces shouldEqual expectedMarketplaces
    observedOutputContext shouldEqual finalContext
  }

  test("the context is not updated when there are no more input updates") {
    val nextInputUpdateStream = mock[SimulationInputUpdateStream]
    nextInputUpdateStream.currentInputUpdate returns None
    firstInputUpdateStream.advance returns nextInputUpdateStream
    val context = fakeContextWithInputs(input(1) -> 1)
    val (newContext, newEnvironment) = environment.advance(context)
    newEnvironment.inputUpdateStream shouldBe nextInputUpdateStream
    newContext shouldEqual context
  }

  test("the simulation is only complete when the input update stream is empty") {
    val nextInputUpdateStream = mock[SimulationInputUpdateStream]
    nextInputUpdateStream.currentInputUpdate returns None
    firstInputUpdateStream.currentInputUpdate returns Some(inputUpdate(input(1) -> 11))
    firstInputUpdateStream.advance returns nextInputUpdateStream
    val context = fakeContextWithInputs(input(1) -> 1)
    val e = environment
    e.isSimulationComplete shouldBe false
    val (_, e2) = e.advance(context)
    e2.isSimulationComplete shouldBe true
  }

}
