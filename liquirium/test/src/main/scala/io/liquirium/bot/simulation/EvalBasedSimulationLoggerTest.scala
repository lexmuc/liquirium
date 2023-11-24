package io.liquirium.bot.simulation

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.helpers.SimpleFakeContext
import io.liquirium.eval.{Eval, EvalResult, UpdatableContext}

/**
 * Superclass of tests for loggers that internally use Evals and can be tested a bit more superficially since it is
 * guaranteed that the context and input requests are correctly handled.
 * @tparam L type of the logger under test
 */
abstract class EvalBasedSimulationLoggerTest [L <: SimulationLogger[L]] extends BasicTest {

  private var context: SimpleFakeContext = SimpleFakeContext(Map())

  protected def initialLogger(): L

  private var logger: L = null.asInstanceOf[L]

  protected var lastLoggingResult: Option[(EvalResult[L], UpdatableContext)] = None

  protected def initLogger(): Unit = {
    logger = initialLogger()
  }

  protected def fakeValues(tuples: (Eval[_], _)*): Unit = {
    context = tuples.foldLeft(context) {
      case (c, t) => c.fake(t._1, t._2)
    }
  }

  protected def log(): Unit = {
    val loggingResult = logger.log(context)
    lastLoggingResult = Some(loggingResult)
    logger = loggingResult._1.get
  }

  protected def fakeValuesAndLog(tuples: (Eval[_], _)*): Unit = {
    fakeValues(tuples: _*)
    log()
  }

  protected def runLogWithEvaluations(tuples: Seq[(Eval[_], _)]): Unit = {
    fakeValues(tuples: _*)
    log()
  }

}
