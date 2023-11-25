package io.liquirium.bot.simulation

import io.liquirium.eval.{EvalResult, InputRequest, UpdatableContext, Value}

import scala.annotation.tailrec

case class AggregateSimulationLogger[T <: SimulationLogger[T]](subLoggers: Seq[T])
  extends SimulationLogger[AggregateSimulationLogger[T]] {

  override def log(context: UpdatableContext): (EvalResult[AggregateSimulationLogger[T]], UpdatableContext) =
    evaluate(context, subLoggers, Seq())

  @tailrec
  private def evaluate(
    context: UpdatableContext,
    restLoggers: Seq[T],
    completeLoggers: Seq[T],
  ): (EvalResult[AggregateSimulationLogger[T]], UpdatableContext) =
    if (restLoggers.isEmpty) {
      (Value(copy(completeLoggers)), context)
    }
    else {
      restLoggers.head.log(context) match {
        case (ir: InputRequest, newContext) => (ir, newContext)
        case (Value(logger), newContext) => evaluate(newContext, restLoggers.tail, completeLoggers :+ logger)
      }
    }

}
