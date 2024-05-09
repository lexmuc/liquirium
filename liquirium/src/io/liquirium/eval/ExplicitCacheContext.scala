package io.liquirium.eval

import scala.annotation.tailrec


trait ExplicitCacheContext extends UpdatableContext {

  override def update(update: InputUpdate): ExplicitCacheContext

}

object ExplicitCacheContext {

  def apply(): ExplicitCacheContext = Impl()

  private object Impl {

    // Some methods are defined in the companion object, so we don't accidentally access members of the wrong context.
    // We are always forced to explicitly denote the context we are working with.

    private def evaluate[M](context: Impl, eval: Eval[M], oldValue: Option[M]): (EvalResult[M], Impl) = {
      val (res, contextAfterEval) = eval match {
        case ce: CachedEval[M] =>
          val (res, newContext) = context.evaluateCachedEval(ce)
          (res, newContext.collectDependencyIfCollecting(ce))
        case de: DerivedEval[M] =>
          de.eval(context, oldValue).asInstanceOf[(EvalResult[M], Impl)]
        case ie: InputEval[M] =>
          val (res, newContext) = context.evaluateInputEval(ie)
          (res, newContext.collectDependencyIfCollecting(ie))
        case Constant(c) => (Value(c), context)
      }
      (res, contextAfterEval)
    }

    private def actuallyEvaluateCachedEvalAndUpdateCacheAndDependencies[M](
      context: Impl,
      ce: CachedEval[M],
    ): (EvalResult[M], Impl) = {
      val (evalResult, contextAfterEvaluation) =
        Impl.evaluate(
          context = context.copy(collectedDependencies = Some(Seq())),
          eval = ce.baseEval,
          oldValue = context.cachedValues.get(ce).flatMap(x => x match {
            case Value(v) => Some(v.asInstanceOf[M])
            case _ => None
          }))
      val newDependencies = contextAfterEvaluation.collectedDependencies.get
      val newContext =
        contextAfterEvaluation
          .saveEvaluation(ce, evalResult, newDependencies)
          .copy(collectedDependencies = context.collectedDependencies)
      (evalResult, newContext)
    }

  }

  private case class Impl(
    inputValues: Map[Input[_], _] = Map(),
    cachedValues: Map[Eval[_], EvalResult[_]] = Map(),
    lastDependencyValues: Map[Eval[_], Seq[(Eval[_], EvalResult[_])]] = Map(),
    dirtyEvals: Set[Eval[_]] = Set(),
    collectedDependencies: Option[Seq[Eval[_]]] = None,
    dependencyGraph: DependencyGraph[Eval[_]] = DependencyGraph.empty,
  ) extends ExplicitCacheContext {

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], Impl) = Impl.evaluate(this, eval, None)

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
          val (hasChanged, newContext) = checkIfValuesHaveChanged(lastDependencyValues(ce))
          if (hasChanged) {
            Impl.actuallyEvaluateCachedEvalAndUpdateCacheAndDependencies(newContext, ce)
          }
          else {
            val evalResult = cachedValues(ce).asInstanceOf[EvalResult[M]]
            (evalResult, newContext)
          }
        }
      } else {
        Impl.actuallyEvaluateCachedEvalAndUpdateCacheAndDependencies(this, ce)
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
