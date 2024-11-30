package io.liquirium.bot

import io.liquirium.bot.BotRunner.ShutdownReason
import io.liquirium.bot.helpers.BotInputHelpers.botInput
import io.liquirium.bot.helpers.OperationRequestHelpers.operationRequestMessage
import io.liquirium.bot.helpers.TestBotEvaluator
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, UnsafeTestSource}
import io.liquirium.eval.{BaseContext, Context, EvalResult, Input, InputEval, InputUpdate, UpdatableContext, Value}
import io.liquirium.eval.helpers.EvalHelpers.inputRequest
import org.mockito.Mockito._

class BotRunnerTest extends AsyncTestWithControlledTime with TestWithMocks {

  protected val runner: BotRunner = BotRunner(spawner)

  protected val inputProvider: BotInputProvider = mock(classOf[BotInputProvider])

  protected val outputProcessor: BotOutputProcessor = mock(classOf[BotOutputProcessor])
  outputProcessor.processOutput(*) returns true

  protected def fakeInputSource[T](input: BotInput[T]): UnsafeTestSource[T] = {
    val testSource = new UnsafeTestSource[T]()
    inputProvider.getInputUpdateStream(input) returns Some(testSource.source)
    testSource
  }

  protected def fakeNoInputSource[T](input: BotInput[T]): Unit = {
    inputProvider.getInputUpdateStream(input) returns None
  }

  protected var regularEffectByContext:
    Map[UpdatableContext, (EvalResult[Seq[BotOutput]], UpdatableContext)] = Map()

  protected var exceptionsByContext: Map[Context, Throwable] = Map()

  protected val evaluator: TestBotEvaluator = TestBotEvaluator(c => {
    if (exceptionsByContext.contains(c)) {
      throw exceptionsByContext(c)
    }
    else regularEffectByContext(c)
  })

  protected var shutdownReason: Option[ShutdownReason] = None

  protected def fakeEvalEffectsForContext(context: UpdatableContext)(
    evalResult: EvalResult[Seq[BotOutput]],
    newContext: UpdatableContext,
  ): Unit = {
    regularEffectByContext = regularEffectByContext.updated(context, (evalResult, newContext))
  }

  protected def out(n: Int): BotOutput = operationRequestMessage(n)

  protected def fakeFailureForNextEvaluation(t: Throwable): Unit = {
    exceptionsByContext = exceptionsByContext.updated(context, t)
  }

  protected def expectOutputs(outputs: BotOutput*): Unit = {
    outputs.foreach { o =>
      verify(outputProcessor).processOutput(o)
    }
  }

  protected def expectInputRequests(inputs: BotInput[_]*): Unit = {
    inputs.foreach { i =>
      verify(inputProvider).getInputUpdateStream(i)
    }
  }

  protected def runBot(context: UpdatableContext): Unit = {
    runner.run(
      evaluator = evaluator,
      inputProvider = inputProvider,
      outputProcessor = outputProcessor,
      shutdownHandler = { sr =>
        shutdownReason = Some(sr)
      },
      context = context,
    )
  }

  case object EvalCounterInput extends Input[Int]
  val firstContext: BaseContext = BaseContext(Map(EvalCounterInput -> 0))

  var context: UpdatableContext = firstContext

  def fakeEffects(evalResult: EvalResult[Seq[BotOutput]]): Unit = {
    val evalCounter = context.evaluate(InputEval(EvalCounterInput))._1.get
    val nextContext = context.update(InputUpdate(Map(
      EvalCounterInput -> (evalCounter + 1)
    )))
    fakeEvalEffectsForContext(context)(evalResult, nextContext)
    context = nextContext
  }

  def fakeInputRequests(i: Input[_], ii: Input[_]*): Unit = fakeEffects(inputRequest(ii :+ i: _*))
  def fakeOutputs(oo: BotOutput*): Unit = fakeEffects(Value(oo))

  def assumeUpdatedInputsForNextRun(updates: (Input[_], Any)*): Unit = {
    context = context.update(InputUpdate(updates.toMap))
  }

  def start(): Unit = runBot(firstContext)

  val inputSource1: UnsafeTestSource[Int] = fakeInputSource(botInput(1))
  val inputSource2: UnsafeTestSource[Int] = fakeInputSource(botInput(2))
  val inputSource3: UnsafeTestSource[Int] = fakeInputSource(botInput(3))

}
