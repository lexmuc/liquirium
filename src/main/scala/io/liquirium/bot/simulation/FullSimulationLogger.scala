package io.liquirium.bot.simulation

import io.liquirium.eval.{Context, EvalResult, UpdatableContext, Value}

case class FullSimulationLogger(reverseUpdates: List[Context] = List())
  extends SimulationLogger[FullSimulationLogger] {

  def log(context: UpdatableContext): (EvalResult[FullSimulationLogger], UpdatableContext) =
    (Value(copy(context :: reverseUpdates)), context)

  def allUpdates: Seq[Context] = reverseUpdates.reverse

}
