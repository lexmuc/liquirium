package io.liquirium.bot.simulation.helpers

import io.liquirium.bot.simulation.SimulationLogger
import io.liquirium.eval._

case class FakeSimulationLogger(
  eval: Eval[Int],
  collectedValues: Seq[Int] = Seq(),
) extends SimulationLogger[FakeSimulationLogger] {

  override def log(context: UpdatableContext): (EvalResult[FakeSimulationLogger], UpdatableContext) = {
    val (result, newContext) = context.evaluate(eval)
    result match {
      case ir: InputRequest => (ir, newContext)
      case Value(v) => (Value(copy(collectedValues = collectedValues :+ v)), newContext)
    }
  }

}
