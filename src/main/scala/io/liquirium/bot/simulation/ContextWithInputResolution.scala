package io.liquirium.bot.simulation

import io.liquirium.eval.{Context, Eval, EvalResult, Input, InputRequest, InputUpdate, UpdatableContext, Value}

case class ContextWithInputResolution(
  baseContext: UpdatableContext,
  resolve: Input[_] => Option[Any],
) extends UpdatableContext {

  override def update(update: InputUpdate): ContextWithInputResolution = copy(baseContext.update(update))

  override def evaluate[M](eval: Eval[M]): (EvalResult[M], ContextWithInputResolution) =
    baseContext.evaluate(eval) match {
      case (Value(x), contextAfterFirstEvaluation) => (Value(x), copy(contextAfterFirstEvaluation))
      case (InputRequest(inputs), newContext) =>
        val inputsWithOptionalValues = inputs.map { i => (i, resolve(i)) }
        val unresolvedInputs = inputsWithOptionalValues.collect { case (i, None) => i }
        if (unresolvedInputs.nonEmpty) {
          (InputRequest(unresolvedInputs), copy(newContext))
        }
        else {
          val update = inputsWithOptionalValues.toMap.mapValues(_.get)
          copy(newContext.update(InputUpdate(update))).evaluate(eval)
        }
    }

}
