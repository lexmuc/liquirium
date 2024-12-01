package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput.CompletedOperationRequest
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.eval.helpers.ContextHelpers.inputUpdate
import io.liquirium.eval.helpers.EvalHelpers.inputRequest
import io.liquirium.eval.{IncrementalSeq, Input, UpdatableContext}
import org.mockito.Mockito.mock

class DynamicInputSimulationEnvironmentTest extends TestWithMocks {

  protected var firstInputUpdateStream: SimulationInputUpdateStream = mock(classOf[SimulationInputUpdateStream])
  // keep track of the current stream to fake when faking several stages
  protected var latestFakeInputStream: SimulationInputUpdateStream = firstInputUpdateStream

  protected var completedTradeRequests: IncrementalSeq[CompletedOperationRequest] = IncrementalSeq.empty

  protected val firstMarketplaces: SimulationMarketplaces = mock(classOf[SimulationMarketplaces])
  protected var expectedMarketplaces: SimulationMarketplaces = firstMarketplaces

  def fakeMarketplacesOrderFill(inputContext: UpdatableContext, outputContext: UpdatableContext): Unit = {
    expectedMarketplaces = mock(classOf[SimulationMarketplaces])
    firstMarketplaces.executeActivatedOrders(inputContext) returns Right((outputContext, expectedMarketplaces))
  }

  protected def fakeInputStreamTransition(requestedInputs: Input[_]*)(pairs: (Input[_], _)*): Unit = {
    val newStream = mock(classOf[SimulationInputUpdateStream])
    newStream.currentInputUpdate returns Some(inputUpdate(pairs: _*))
    latestFakeInputStream.processInputRequest(inputRequest(requestedInputs: _*)) returns newStream
    latestFakeInputStream = newStream
  }

  def environment: DynamicInputSimulationEnvironment =
    DynamicInputSimulationEnvironment(firstInputUpdateStream, firstMarketplaces)

}
