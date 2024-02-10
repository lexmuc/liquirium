package io.liquirium.bot

import io.liquirium.bot.BotInput.BotOutputHistory
import io.liquirium.bot.helpers.BotHelpers.{botOutput => out}
import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval._
import io.liquirium.eval.helpers.EvalHelpers.{inputRequest, testEval}


class SimpleBotEvaluatorTest extends BasicTest {

  type TraderEvalResult = EvalResult[Iterable[BotOutput]]

  val finalContext: UpdatableContext = BaseContext.fromInputValues(Map())
  val outputHistoryEval: InputEval[IncrementalSeq[BotOutput]] = InputEval(BotOutputHistory)

  private val botEval = testEval(123)

  val evaluator: BotEvaluator = SimpleBotEvaluator(botEval)

  case class FakeContext(
    expectedEval: Eval[_],
    result: EvalResult[_],
    newContext: UpdatableContext,
    recordedUpdateMappings: Map[Input[_], Any] = Map(),
  ) extends UpdatableContext {

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], UpdatableContext) =
      eval match {
        case x if x == expectedEval =>
          (result.asInstanceOf[EvalResult[M]], newContext.update(InputUpdate(recordedUpdateMappings)))
        case e: DerivedEval[_] =>
          val (evalResult, newContext) = e.eval(this, None)
          (evalResult.asInstanceOf[EvalResult[M]], newContext.asInstanceOf[FakeContext])
        case _ =>
          fail("FakeContext received unexpected eval: " + eval + "; expected " + expectedEval)
      }

      def update(update: InputUpdate): FakeContext = {
        copy(recordedUpdateMappings = recordedUpdateMappings ++ update.updateMappings)
      }

  }

  test("in case of no output or input request it just returns the context without bot outputs") {
    val history = IncrementalSeq(out(1))
    val c1 = FakeContext(botEval, Value(Seq()), finalContext)
    val c0 = FakeContext(outputHistoryEval, Value(history), c1)
    evaluator.eval(c0) shouldEqual (Value(Seq[BotOutput]()), finalContext)
  }

  test("if the output history cannot be obtained, it is set to empty and the bot is evaluated") {
    val c1 = FakeContext(botEval, Value(Seq(out(1))), finalContext)
    val c0 = FakeContext(outputHistoryEval, inputRequest(1), c1)
    val expectedContext = finalContext.update(InputUpdate(Map(BotOutputHistory -> IncrementalSeq(out(1)))))
    evaluator.eval(c0) shouldEqual (Value(Seq(out(1))), expectedContext)
  }

  test("in case the bot outputs something it is appended to the history and returned in the eval result") {
    val history = IncrementalSeq(out(1))
    val c1 = FakeContext(botEval, Value(Seq(out(2), out(3))), finalContext)
    val c0 = FakeContext(outputHistoryEval, Value(history), c1)
    val expectedHistory = history.inc(out(2)).inc(out(3))
    val expectedContext =  finalContext.update(InputUpdate(Map(BotOutputHistory -> expectedHistory)))
    evaluator.eval(c0) shouldEqual (Value(Seq(out(2), out(3))), expectedContext)
  }

  test("if the bot output yields an input request it is returned with latest context (not updated)") {
    val history = IncrementalSeq(out(1))
    val c1 = FakeContext(botEval, inputRequest(123), finalContext)
    val c0 = FakeContext(outputHistoryEval, Value(history), c1)
    evaluator.eval(c0) shouldEqual (inputRequest(123), finalContext)
  }

}
