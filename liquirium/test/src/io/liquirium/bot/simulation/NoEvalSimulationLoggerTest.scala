package io.liquirium.bot.simulation

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.helpers.EvalHelpers.inputRequest
import io.liquirium.eval.{BaseContext, Context, DerivedEval, Eval, EvalResult, InputUpdate, UpdatableContext, Value}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.annotation.tailrec

/**
 * Super class for some of the logger tests
 *
 * A logger should make use of Evals so it is guaranteed that input requests and the context itself is passed
 * along correctly. This test is for the other loggers that do not use Evals yet.
 *
 */
abstract class NoEvalSimulationLoggerTest[L <: SimulationLogger[L]] extends BasicTest {

  val finalContext: UpdatableContext = BaseContext.fromInputValues(Map())

  private var context: UpdatableContext = finalContext

  protected def initialLogger(): L

  private var logger: L = null.asInstanceOf[L]

  protected var lastLoggingResult: Option[(EvalResult[L], UpdatableContext)] = None

  protected def initLogger(): Unit = {
    logger = initialLogger()
  }

  protected def log(): Unit = {
    val loggingResult = logger.log(context)
    lastLoggingResult = Some(loggingResult)
    logger = loggingResult._1.get
  }

  case class FakeContext(
    expectedEval: Eval[_],
    result: EvalResult[_],
    newContext: UpdatableContext,
  ) extends UpdatableContext {

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], UpdatableContext) =
      eval match {
        case x if x == expectedEval =>
          (result.asInstanceOf[EvalResult[M]], newContext)
        case e: DerivedEval[_] =>
          val (evalResult, newContext) = e.eval(this, None)
          (evalResult.asInstanceOf[EvalResult[M]], newContext.asInstanceOf[UpdatableContext])
        case _ =>
          fail("FakeContext received unexpected eval: " + eval + "; expected " + expectedEval)
      }

    override def update(update: InputUpdate): UpdatableContext = ???

  }

  @tailrec
  private def assertInputRequestsAreForwarded(
    tuples: Seq[(Eval[_], _)],
    restTuples: Seq[(Eval[_], _)] = Seq(),
  ): Unit = {
    if (tuples.nonEmpty) {
      val mappedTuples = tuples.map(t => (t._1, Value(t._2)))
      val mappedRestTuples = restTuples.map(t => (t._1, Value(t._2)))
      val fakeTuples = (mappedTuples.init :+ (tuples.last._1, inputRequest(1))) ++ mappedRestTuples
      context = makeFakeContextForEvaluations(fakeTuples)
      val loggingResult = logger.log(context)
      loggingResult._1 shouldEqual inputRequest(1)
      assertFinalContextIsValid(loggingResult._2, mappedRestTuples)
      val newRest = tuples.last +: restTuples
      assertInputRequestsAreForwarded(tuples.init, newRest)
    }
  }

  @tailrec
  private def assertFinalContextIsValid(
    context: Context,
    restTuples: Seq[(Eval[_], Value[_])] = Seq(),
  ): Unit = {
    if (context != makeFakeContextForEvaluations(restTuples)) {
      if (restTuples.isEmpty) {
        fail("Context returned by logger is not the latest context that has recorded all evaluations so far")
      }
      else {
        assertFinalContextIsValid(context, restTuples.tail)
      }
    }
  }

  private def makeFakeContextForEvaluations(tuples: Seq[(Eval[_], EvalResult[_])]): UpdatableContext =
    tuples.reverse.foldLeft(finalContext) { (c, t) =>
      FakeContext(t._1, t._2, c)
    }

  protected def runLogWithEvaluations(tuples: Seq[(Eval[_], _)]): Unit = {
    assertInputRequestsAreForwarded(tuples)
    context = makeFakeContextForEvaluations(tuples.map(t => (t._1, Value(t._2))))
    log()
    lastLoggingResult.get._2 shouldEqual finalContext
  }

}
