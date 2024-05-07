package io.liquirium.eval

import scala.annotation.tailrec


trait ExplicitCacheContext extends UpdatableContext {

  override def update(update: InputUpdate): ExplicitCacheContext

}

object ExplicitCacheContext {

  def apply(): ExplicitCacheContext = Impl(
    inputValues = Map(),
    cachedValues = Map(),
    lastDependencyValues = Map(),
    dirtyEvals = Set(),
    collectedDependencies = None,
    dependencyGraph = DependencyGraph.empty,
  )

  private case class Impl(
    inputValues: Map[Input[_], _],
    cachedValues: Map[Eval[_], EvalResult[_]],
    lastDependencyValues: Map[Eval[_], Seq[(Eval[_], EvalResult[_])]],
    dirtyEvals: Set[Eval[_]],
    collectedDependencies: Option[Seq[Eval[_]]],
    dependencyGraph: DependencyGraph[Eval[_]],
  ) extends ExplicitCacheContext {

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], Impl) = doEvaluate(eval, None)

    private def doEvaluate[M](eval: Eval[M], oldValue: Option[M]): (EvalResult[M], Impl) = {
      val (res, contextAfterEval) = eval match {
        case ce: CachedEval[M] =>
          val (res, ctx) = evaluateCachedEval(ce)
          (res, ctx.collectDependencyIfCollecting(ce))
        case de: DerivedEval[M] =>
          de.eval(this, None).asInstanceOf[(EvalResult[M], Impl)]
        case ie: InputEval[M] =>
          val (res, ctx) = evaluateInputEval(ie)
          (res, ctx.collectDependencyIfCollecting(ie))
        case Constant(c) => (Value(c), this)
      }
      (res, contextAfterEval)
    }

    private def evaluateInputEval[M](inputEval: InputEval[M]): (EvalResult[M], Impl) = {
      val input = inputEval.input
      if (inputValues.keySet(input)) (Value(inputValues(input).asInstanceOf[M]), this)
      else (InputRequest(Set(input)), this)
    }

    private def collectDependencyIfCollecting(eval: Eval[_]): Impl =
      if (collectedDependencies.isDefined) copy(collectedDependencies = collectedDependencies.map(_ :+ eval))
      else this

    private def evaluateCachedEval[M](ce: CachedEval[M]): (EvalResult[M], Impl) =
      if (cachedValues.contains(ce)) {
        if (!dirtyEvals(ce)) {
          (cachedValues(ce).asInstanceOf[EvalResult[M]], this)
        } else {
          val (hasChanged, newContext) = checkIfValuesHaveChanged(lastDependencyValues(ce).toSeq)
          if (hasChanged) {
            newContext.actuallyEvaluateCachedEvalAndUpdateCacheAndDependencies(ce)
          }
          else {
            val evalResult = cachedValues(ce).asInstanceOf[EvalResult[M]]
            (evalResult, newContext)
          }
        }
      } else {
        actuallyEvaluateCachedEvalAndUpdateCacheAndDependencies(ce)
      }

    private def checkIfValuesHaveChanged(evalsWithLastValues: Seq[(Eval[_], EvalResult[_])]): (Boolean, Impl) = {
      @tailrec
      def go(rest: Seq[(Eval[_], EvalResult[_])], context: Impl): (Boolean, Impl) = {
        if (rest.isEmpty) (false, context)
        else {
          val (eval, lastValue) = rest.head
          val (result, newContext) = context.evaluate(eval)
          if (result != lastValue) (true, newContext)
          else go(rest.tail, newContext)
        }
      }
      val (resultBool, ctx) = go(evalsWithLastValues, this.copy(collectedDependencies = None))
      (resultBool, ctx.copy(collectedDependencies = this.collectedDependencies)) // restore dependencies
    }

    private def actuallyEvaluateCachedEvalAndUpdateCacheAndDependencies[M](ce: CachedEval[M]): (EvalResult[M], Impl) = {
      val (evalResult, contextAfterEvaluation) = copy(collectedDependencies = Some(Seq())).evaluate(ce.baseEval)
      val newDependencies = contextAfterEvaluation.collectedDependencies.get
      val newContext =
        contextAfterEvaluation
          .saveEvaluation(ce, evalResult, newDependencies)
          .copy(collectedDependencies = this.collectedDependencies)
      (evalResult, newContext)
    }

    private def saveEvaluation(eval: Eval[_], result: EvalResult[_], dependencies: Seq[Eval[_]]) =
      copy(
        cachedValues = cachedValues.updated(eval, result),
        dirtyEvals = dirtyEvals - eval,
        dependencyGraph = dependencyGraph.setNodeDependencies(eval, dependencies.toSet),
        lastDependencyValues = lastDependencyValues.updated(eval, getDependenciesWithValues(dependencies)),
      )

    private def getDependenciesWithValues(dependencies: Seq[Eval[_]]): Seq[(Eval[_], EvalResult[_])] =
      dependencies.map {
        case ie@InputEval(i) if inputValues.contains(i) => ie -> Value(inputValues(i))
        case ie@InputEval(i) => ie -> InputRequest(Set(i))
        case ce@CachedEval(_) => ce -> cachedValues(ce)
      }

    override def update(update: InputUpdate): ExplicitCacheContext =
      update.updateMappings.foldLeft(this)((c, uv) => c.updateSingleInput(uv._1, uv._2))

    private def updateSingleInput(input: Input[_], value: Any): Impl =
      copy(
        inputValues = inputValues.updated(input, value),
        dirtyEvals = dirtyEvals ++ dependencyGraph.getAllDependents(InputEval(input)),
      )

  }
}
