package io.liquirium.eval.helpers

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval._


abstract class EvalTestWithIncrementalContext[T] extends BasicTest {

  private var context: UpdatableContext = IncrementalContext()

  protected def fakeInputEval[T]: InputEval[T] = {
    object FakeInput extends Input[T]
    InputEval(FakeInput)
  }

  protected def evalUnderTest: Eval[T]

  protected def updateInput[T](inputEval: InputEval[T], value: T): Unit = {
    context = context.update(InputUpdate(Map(inputEval.input -> value)))
  }

  protected def eval(): T = {
    val (evaluationResult, newContext) = context.evaluate(evalUnderTest)
    context = newContext
    evaluationResult match {
      case Value(v) => v
      case ir: InputRequest =>
        throw new RuntimeException(s"Unexpected input request $ir. All inputs should have been supplied.")
    }
  }

}
