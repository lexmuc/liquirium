package io.liquirium.eval


object BaseContext {

  def fromInputValues(inputs: Map[Input[_], _]): BaseContext = BaseContext(inputValues = inputs)

  val empty: BaseContext = BaseContext(Map())

}

case class BaseContext(inputValues: Map[Input[_], _])
  extends UpdatableContext {

  def evaluate[M](eval: Eval[M]): (EvalResult[M], BaseContext) = {
    val er = eval match {
      case Constant(x) => Value(x)
      case dm: DerivedEval[M] => dm.eval(this, None)._1
      case InputEval(input) =>
        if (inputValues.keySet(input)) Value(inputValues(input).asInstanceOf[M])
        else InputRequest(Set(input))
    }
    (er, this)
  }

  override def update(update: InputUpdate): UpdatableContext =
    copy(inputValues = inputValues ++ update.updateMappings)

}
