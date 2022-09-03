package io.liquirium.eval


trait Context {

  def evaluate[M](eval: Eval[M]): (EvalResult[M], Context)

  def apply[M](eval: Eval[M]): EvalResult[M] = evaluate(eval)._1

}

trait UpdatableContext extends Context {

  def update(update: InputUpdate): UpdatableContext

  override def evaluate[M](eval: Eval[M]): (EvalResult[M], UpdatableContext)

}
