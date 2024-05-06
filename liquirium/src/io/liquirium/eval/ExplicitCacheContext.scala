package io.liquirium.eval


trait ExplicitCacheContext extends UpdatableContext {

  override def update(update: InputUpdate): ExplicitCacheContext

}

object ExplicitCacheContext {

  def apply(): ExplicitCacheContext = Impl(Map(), Map(), Set(), DependencyGraph.empty)

  private case class Impl(
    inputValues: Map[Input[_], _],
    cachedValues: Map[Eval[_], EvalResult[_]],
    collectedDependencies: Set[Eval[_]],
    dependencyGraph: DependencyGraph[Eval[_]],
  ) extends ExplicitCacheContext {

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], Impl) = doEvaluate(eval, None)

    private def doEvaluate[M](eval: Eval[M], oldValue: Option[M]): (EvalResult[M], Impl) = {
      val (res, contextAfterEval) = eval match {
        case ce: CachedEval[M] => evaluateCachedEval(ce)
        case de: DerivedEval[M] => de.eval(this, None).asInstanceOf[(EvalResult[M], Impl)]
        case ie: InputEval[M] => evaluateInputEval(ie)
        case Constant(c) => (Value(c), this)
      }
      (res, contextAfterEval)
    }

    private def evaluateInputEval[M](ie: InputEval[M]): (EvalResult[M], Impl) = {
      val input = ie.input
      val contextWithNewDependency = copy(collectedDependencies = collectedDependencies + ie)
      if (inputValues.keySet(input)) (Value(inputValues(input).asInstanceOf[M]), contextWithNewDependency)
      else (InputRequest(Set(input)), contextWithNewDependency)
    }

    private def evaluateCachedEval[M](ce: CachedEval[M]): (EvalResult[M], Impl) = {
      if (cachedValues.contains(ce)) {
        (cachedValues(ce).asInstanceOf[EvalResult[M]], copy(collectedDependencies = collectedDependencies + ce))
      } else {
        val (evalResult, contextAfterEvaluation) = copy(collectedDependencies = Set()).evaluate(ce.baseEval)
        val newContext =
          contextAfterEvaluation.copy(
            cachedValues = contextAfterEvaluation.cachedValues.updated(ce, evalResult),
            dependencyGraph = contextAfterEvaluation.dependencyGraph
              .setNodeDependencies(ce, contextAfterEvaluation.collectedDependencies),
            collectedDependencies = this.collectedDependencies + ce,
          )
        (evalResult, newContext)
      }
    }

    override def update(update: InputUpdate): ExplicitCacheContext = {
      update.updateMappings.foldLeft(this)((c, uv) => c.updateSingleInput(uv._1, uv._2))
    }

    private def updateSingleInput(input: Input[_], value: Any): Impl = {
      val affectedEvals = dependencyGraph.getAllDependents(InputEval(input))
      copy(
        inputValues = inputValues.updated(input, value),
        cachedValues = cachedValues -- affectedEvals,
      )
    }

  }
}
