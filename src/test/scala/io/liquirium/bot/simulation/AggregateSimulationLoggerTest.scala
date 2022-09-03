package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.helpers.FakeSimulationLogger
import io.liquirium.eval.Eval
import io.liquirium.eval.helpers.EvalHelpers.testEval

class AggregateSimulationLoggerTest extends SimulationLoggerTest[AggregateSimulationLogger[FakeSimulationLogger]] {

  var subLoggers: Seq[FakeSimulationLogger] = Seq()

  val mA: Eval[Int] = testEval(1)
  val mB: Eval[Int] = testEval(2)

  override protected def initialLogger(): AggregateSimulationLogger[FakeSimulationLogger] =
    AggregateSimulationLogger(subLoggers)

  test("it simply returns the logger as is if there are no sub loggers") {
    subLoggers = Seq()
    initLogger()
    runLogWithEvaluations(Seq())
    lastLoggingResult.get._1.get shouldEqual initialLogger()
  }

  test("logging an update yields a new logger with respectively updated sub-loggers") {
    val testLoggerA = FakeSimulationLogger(mA)
    val testLoggerB = FakeSimulationLogger(mB)
    subLoggers = Seq(testLoggerA, testLoggerB)
    initLogger()
    runLogWithEvaluations(Seq(mA -> 11, mB -> 22))
    lastLoggingResult.get._1.get.subLoggers shouldEqual Seq(
      FakeSimulationLogger(mA, Seq(11)),
      FakeSimulationLogger(mB, Seq(22)),
    )
  }

}
