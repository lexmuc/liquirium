package io.liquirium.bot.simulation

import io.liquirium.bot.simulation.helpers.FakeSimulationLogger
import io.liquirium.eval.Eval
import io.liquirium.eval.helpers.EvalHelpers.testEval

class CombinedSimulationLoggerTest
  extends SimulationLoggerTest[CombinedSimulationLogger[FakeSimulationLogger, FakeSimulationLogger]] {

  val mA: Eval[Int] = testEval(1)
  val mB: Eval[Int] = testEval(2)

  val testLoggerA: FakeSimulationLogger = FakeSimulationLogger(mA)
  val testLoggerB: FakeSimulationLogger = FakeSimulationLogger(mB)

  override protected def initialLogger(): CombinedSimulationLogger[FakeSimulationLogger, FakeSimulationLogger] =
    CombinedSimulationLogger(testLoggerA, testLoggerB)

  test("logging an update yields a new logger with respectively updated sub-loggers") {
    initLogger()
    runLogWithEvaluations(Seq(mA -> 11, mB -> 22))
    lastLoggingResult.get._1.get.loggerA shouldEqual FakeSimulationLogger(mA, Seq(11))
    lastLoggingResult.get._1.get.loggerB shouldEqual FakeSimulationLogger(mB, Seq(22))
  }

}
