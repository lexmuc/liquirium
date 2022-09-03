package io.liquirium.bot.simulation

import io.liquirium.eval.{EvalResult, InputRequest, UpdatableContext, Value}

case class CombinedSimulationLogger[A <: SimulationLogger[A], B <: SimulationLogger[B]](
  loggerA: A,
  loggerB: B,
) extends SimulationLogger[CombinedSimulationLogger[A, B]] {

  override def log(context: UpdatableContext): (EvalResult[CombinedSimulationLogger[A, B]], UpdatableContext) = {
    loggerA.log(context) match {
      case (ir: InputRequest, newContext) => (ir, newContext)
      case (Value(newLoggerA), newContext) =>
        loggerB.log(newContext) match {
          case (ir: InputRequest, contextAfterB) => (ir, contextAfterB)
          case (Value(newLoggerB), contextAfterB) =>
            val value = Value(copy(
              loggerA = newLoggerA,
              loggerB = newLoggerB,
            ))
            (value, contextAfterB)
        }
    }
  }

}
