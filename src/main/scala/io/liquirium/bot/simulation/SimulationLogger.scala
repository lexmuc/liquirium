package io.liquirium.bot.simulation

import io.liquirium.eval.{EvalResult, UpdatableContext}

trait SimulationLogger[T <: SimulationLogger[T]] {

  def log(context: UpdatableContext): (EvalResult[T], UpdatableContext)

}