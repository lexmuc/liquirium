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
    lastDependencyValues: Map[Eval[_], Map[Eval[_], EvalResult[_]]],
    dirtyEvals: Set[Eval[_]],
    collectedDependencies: Option[Set[Eval[_]]],
    dependencyGraph: DependencyGraph[Eval[_]],
  ) extends ExplicitCacheContext {

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], Impl) = doEvaluate(eval, None)

    private def doEvaluate[M](eval: Eval[M], oldValue: Option[M]): (EvalResult[M], Impl) = {
      println()
      println("evaluating " + eval)
      println("current collected dependencies: " + collectedDependencies)

      val (res, contextAfterEval) = eval match {
        case ce: CachedEval[M] => evaluateCachedEval(ce)
        case de: DerivedEval[M] => de.eval(this, None).asInstanceOf[(EvalResult[M], Impl)]
        case ie: InputEval[M] => evaluateInputEval(ie)
        case Constant(c) => (Value(c), this)
      }
      (res, contextAfterEval)
    }

    private def evaluateInputEval[M](inputEval: InputEval[M]): (EvalResult[M], Impl) = {
      val input = inputEval.input
      val contextWithNewDependency = collectDependencyIfCollecting(inputEval)
      if (inputValues.keySet(input)) (Value(inputValues(input).asInstanceOf[M]), contextWithNewDependency)
      else (InputRequest(Set(input)), contextWithNewDependency)
    }

    private def collectDependencyIfCollecting(eval: Eval[_]): Impl =
      if (collectedDependencies.isDefined) copy(collectedDependencies = collectedDependencies.map(_ + eval))
      else this

    private def evaluateCachedEval[M](ce: CachedEval[M]): (EvalResult[M], Impl) = {
      println("")
      println("evaluating cached eval " + ce)
      if (cachedValues.contains(ce)) {
        if (!dirtyEvals(ce)) {
          println("not dirty")
          (cachedValues(ce).asInstanceOf[EvalResult[M]], collectDependencyIfCollecting(ce))
        } else {
          val (hasChanged, newContext) = checkIfValuesHaveChanged(lastDependencyValues(ce).toSeq)
          println("last dependencies: " + lastDependencyValues(ce))
          if (hasChanged) {
            println("dependencies changed: " + hasChanged)
            newContext.actuallyEvaluateCachedEvalAndUpdateCacheAndDependencies(ce)
          }
          else {
            println("dependencies changed: " + hasChanged)
            val evalResult = cachedValues(ce).asInstanceOf[EvalResult[M]]
            (evalResult, newContext.collectDependencyIfCollecting(ce)) // #TODO does a test fail if I don't update collected dependencies?
            //(evalResult, newContext) // #TODO does a test fail if I don't update collected dependencies?
          }
        }
      } else {
        println("new")
        actuallyEvaluateCachedEvalAndUpdateCacheAndDependencies(ce)
      }
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
      println("---")
      println("starting to evaluate cached eval " + ce)
      println("current collected dependencies: " + collectedDependencies)
      println("---")
      val (evalResult, contextAfterEvaluation) = copy(collectedDependencies = Some(Set())).evaluate(ce.baseEval)
      val newDependencies = contextAfterEvaluation.collectedDependencies.get
      println("saving evaluation for " + ce + " with dependencies " + newDependencies)
      val newContext =
        contextAfterEvaluation
          .saveEvaluation(ce, evalResult, newDependencies)
          .copy(collectedDependencies = this.collectedDependencies)
      println("new context has dependencies " + newContext.collectedDependencies)
      val r = (evalResult, newContext.collectDependencyIfCollecting(ce))
      println("returning with dependencies " + r._2.collectedDependencies + " when evaluating " + ce)
      r
    }

    private def saveEvaluation(eval: Eval[_], result: EvalResult[_], dependencies: Set[Eval[_]]) =
      copy(
        cachedValues = cachedValues.updated(eval, result),
        dirtyEvals = dirtyEvals - eval,
        dependencyGraph = dependencyGraph.setNodeDependencies(eval, dependencies),
        lastDependencyValues = lastDependencyValues.updated(eval, getDependenciesWithValues(dependencies)),
      )

    private def getDependenciesWithValues(dependencies: Set[Eval[_]]): Map[Eval[_], EvalResult[_]] =
      dependencies.toSeq.map {
        case ie@InputEval(i) if inputValues.contains(i) => ie -> Value(inputValues(i))
        case ie@InputEval(i) => ie -> InputRequest(Set(i))
        case ce@CachedEval(_) => ce -> cachedValues(ce)
      }.toMap

    override def update(update: InputUpdate): ExplicitCacheContext = {
      update.updateMappings.foldLeft(this)((c, uv) => c.updateSingleInput(uv._1, uv._2))
    }

    private def updateSingleInput(input: Input[_], value: Any): Impl =
      copy(
        inputValues = inputValues.updated(input, value),
        dirtyEvals = dirtyEvals ++ dependencyGraph.getAllDependents(InputEval(input)),
      )

  }
}
