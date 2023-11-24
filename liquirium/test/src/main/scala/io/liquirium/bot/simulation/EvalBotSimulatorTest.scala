package io.liquirium.bot.simulation

import io.liquirium.bot.helpers.BotOutputHelpers.output
import io.liquirium.bot.{BotEvaluator, BotOutput}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.eval._
import io.liquirium.eval.helpers.EvalHelpers.inputRequest

class EvalBotSimulatorTest extends TestWithMocks {

  // will be mocked anyway but we need a concrete simulation logger
  case class TestLogger(n: Int) extends SimulationLogger[TestLogger] {
    override def log(context: UpdatableContext): (EvalResult[TestLogger], UpdatableContext) = {
      throw new RuntimeException("TestLogger cannot actually log anything")
    }
  }

  case object EvalCounterInput extends Input[Int]
  val firstContext: BaseContext = BaseContext(Map(EvalCounterInput -> 0))
  val firstLogger: TestLogger = mock[TestLogger]
  val firstEnvironment: SimulationEnvironment = mock[SimulationEnvironment]

  var context: UpdatableContext = firstContext

  val evaluator: BotEvaluator = mock[BotEvaluator]
  var lastEnvironment: SimulationEnvironment = firstEnvironment
  var lastLogger: TestLogger = firstLogger

  def advanceContext(): Unit = {
    val evalCounter = context.evaluate(InputEval(EvalCounterInput))._1.get
    context = context.update(InputUpdate(Map(EvalCounterInput -> (evalCounter + 1))))
  }

  def fakeEvaluationInputRequest(ii: Input[_]*): Unit = {
    val inputContext = context
    advanceContext()
    evaluator.eval(inputContext) returns (InputRequest(ii.toSet), context)
  }

  def fakeBotOutput(oo: BotOutput*): Unit = {
    val inputContext = context
    advanceContext()
    evaluator.eval(inputContext) returns (Value(oo), context)
  }

  def fakeBotInputRequest(ir: InputRequest): Unit = {
    val inputContext = context
    advanceContext()
    evaluator.eval(inputContext) returns (ir, context)
  }

  def fakeLogging(): Unit = {
    val inputContext = context
    advanceContext()
    val newLogger = mock[TestLogger]
    lastLogger.log(inputContext) returns (Value(newLogger), context)
    lastLogger = newLogger
  }

  def fakeLoggingInputRequest(ir: InputRequest): Unit = {
    val inputContext = context
    advanceContext()
    lastLogger.log(inputContext) returns (ir, context)
  }

  def fakeInputRequestResolution(ir: InputRequest): Unit = {
    val inputContext = context
    advanceContext()
    val newEnvironment = mock[SimulationEnvironment]
    lastEnvironment.getInputs(ir, inputContext) returns (context, newEnvironment)
    lastEnvironment = newEnvironment
  }

  def fakeOutputProcessing(outputs: BotOutput*): Unit = {
    val inputContext = context
    advanceContext()
    val newEnvironment = mock[SimulationEnvironment]
    lastEnvironment.processOutputs(outputs, inputContext) returns (context, newEnvironment)
    lastEnvironment = newEnvironment
  }

  def fakeEnvironmentAdvance(isComplete: Boolean = false): Unit = {
    val inputContext = context
    advanceContext()
    val newEnvironment = mock[SimulationEnvironment]
    newEnvironment.isSimulationComplete returns isComplete
    lastEnvironment.advance(inputContext) returns (context, newEnvironment)
    lastEnvironment = newEnvironment
  }

  def makeSimulator(): EvalBotSimulator[TestLogger] = EvalBotSimulator(
    context = firstContext,
    evaluator = evaluator,
    environment = firstEnvironment,
    logger = firstLogger,
  )

  def simulateStepAndAssertOutcome(): Unit = {
    val finalSimulator = makeSimulator().simulateOneStep()
    finalSimulator.context shouldBe theSameInstanceAs(context)
    finalSimulator.evaluator shouldBe theSameInstanceAs(evaluator)
    finalSimulator.environment shouldBe theSameInstanceAs(lastEnvironment)
    finalSimulator.logger shouldBe theSameInstanceAs(lastLogger)
  }

  test("the context is passed through the bot, environment, logger, and environment again (for advancing)") {
    fakeBotOutput()
    fakeOutputProcessing()
    fakeLogging()
    fakeEnvironmentAdvance()
    simulateStepAndAssertOutcome()
  }

  test("outputs are passed to the environment but it is not advanced and logging is skipped when there are outputs") {
    fakeBotOutput(output(1), output(2))
    fakeOutputProcessing(output(1), output(2))
    simulateStepAndAssertOutcome()
  }

  test("as long as there are input requests for the bot evaluation they are resolved and evaluation repeated") {
    fakeBotInputRequest(inputRequest(1))
    fakeInputRequestResolution(inputRequest(1))
    fakeBotInputRequest(inputRequest(2))
    fakeInputRequestResolution(inputRequest(2))
    fakeBotOutput(output(1))
    fakeOutputProcessing(output(1))
    simulateStepAndAssertOutcome()
  }

  test("as long as there are logging results in input requests they are resolved and logging is repeated") {
    fakeBotOutput()
    fakeOutputProcessing()
    fakeLoggingInputRequest(inputRequest(1))
    fakeInputRequestResolution(inputRequest(1))
    fakeLoggingInputRequest(inputRequest(2))
    fakeInputRequestResolution(inputRequest(2))
    fakeLogging()
    fakeEnvironmentAdvance()
    simulateStepAndAssertOutcome()
  }

  test("when the simulation is run the final logger is returned when the environment is complete") {
    fakeBotOutput()
    fakeOutputProcessing()
    fakeLogging()
    fakeEnvironmentAdvance(isComplete = false)

    fakeBotOutput(output(1))
    fakeOutputProcessing(output(1))

    fakeBotOutput()
    fakeOutputProcessing()
    fakeLogging()
    fakeEnvironmentAdvance(isComplete = true)

    makeSimulator().run() shouldBe theSameInstanceAs(lastLogger)
  }

}
