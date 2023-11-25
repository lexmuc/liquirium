package io.liquirium.eval.helpers

import io.liquirium.eval.helpers.EvalHelpers.inputRequest
import io.liquirium.eval.{Eval, EvalResult, InputEval, InputUpdate, UpdatableContext, Value}

case class TestContextWithMockedEval
(
  inputValues: ContextHelpers.InputValues,
  mockedEval: (Eval[_], ContextHelpers.InputValues) => EvalResult[_]
) extends UpdatableContext {

  override def update(update: InputUpdate): TestContextWithMockedEval = copy(inputValues ++ update.updateMappings)

  override def evaluate[M](eval: Eval[M]): (EvalResult[M], TestContextWithMockedEval) = {
    val er = eval match {
      case InputEval(i) if inputValues.keySet(i) => Value(inputValues(i).asInstanceOf[M])
      case InputEval(i) => inputRequest(i)
      case m => mockedEval(m, inputValues).asInstanceOf[EvalResult[M]]
    }
    (er, this)
  }

}
