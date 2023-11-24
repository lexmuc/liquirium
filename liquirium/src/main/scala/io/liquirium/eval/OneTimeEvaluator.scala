package io.liquirium.eval


import scala.concurrent.{ExecutionContext, Future}

case class OneTimeEvaluator(inputProvider: InputProvider) {

  def apply[M](eval: Eval[M], context: UpdatableContext)(
    implicit ec: ExecutionContext
  ): Future[(M, UpdatableContext)] =
    context.evaluate(eval) match {
      case (Value(x), newContext) => Future {
        (x, newContext)
      }
      case (InputRequest(inputs), newContext) =>
        val inputFutureOptionPairs = inputs.toSeq.map { i => (i, inputProvider(i)) }
        val unknownInputs = inputFutureOptionPairs.collect { case (i, None) => i }.toSet
        if (unknownInputs.nonEmpty) Future.failed(UnknownInputsException(unknownInputs))
        else {
          val futuresOfInputValuePairs = inputFutureOptionPairs.collect { case (i, Some(f)) => f.map(v => (i, v)) }
          Future.sequence(futuresOfInputValuePairs).flatMap { pairs =>
            apply(eval, newContext.update(InputUpdate(pairs.toMap)))
          }
        }
    }

}
