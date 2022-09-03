package io.liquirium.eval.helpers

import io.liquirium.eval._


case class SimpleFakeContext(mappings: Map[Eval[_], EvalResult[_]]) extends UpdatableContext {

  override def update(update: InputUpdate): SimpleFakeContext = {
    val inputEvals = update.updateMappings.map { case (k, v) => (InputEval(k), Value(v)) }
    copy(mappings ++ inputEvals)
  }

  def evaluate[M](eval: Eval[M]): (EvalResult[M], SimpleFakeContext) = {
    val er = eval match {
      case e: Eval[M] if mappings.contains(e) => mappings(e).asInstanceOf[EvalResult[M]]
      case Constant(x) => Value(x)
      case dm: DerivedEval[M] => dm.eval(this, None)._1
      case InputEval(input) => InputRequest(Set(input))
    }
    (er, this)
  }

  def fake[E](input: Input[E], v: E): SimpleFakeContext = copy(
    mappings = mappings.updated(InputEval(input), Value(v)),
  )

  def fake[E](e: Eval[E], v: E): SimpleFakeContext = copy(
    mappings = mappings.updated(e, Value(v)),
  )

  def fakeMissing(input: Input[_]): SimpleFakeContext = copy(
    mappings = mappings - InputEval(input),
  )

  def fakeMissingInputs[E](e: Eval[E], ir: InputRequest): SimpleFakeContext = copy(
    mappings = mappings.updated(e, ir)
  )

}
