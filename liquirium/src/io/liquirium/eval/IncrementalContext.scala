package io.liquirium.eval


trait IncrementalContext extends UpdatableContext {

  override def update(update: InputUpdate): IncrementalContext

}

object IncrementalContext {

  def apply(): IncrementalContext = Impl(Map(), Map(), None, DependencyGraph.empty)

  private case class Impl(
    inputValues: Map[Input[_], _],
    cachedValues: Map[Eval[_], EvalResult[_]],
    collectedDependencies: Option[Set[Eval[_]]],
    dependencyGraph: DependencyGraph[Eval[_]],
  ) extends IncrementalContext {

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], Impl) = doEvaluate(eval, None)

    private def doEvaluate[M](eval: Eval[M], oldValue: Option[M]): (EvalResult[M], Impl) = {
      val (res, contextAfterEval) = eval match {
        case de: DerivedEval[M] if cachedValues.contains(de) =>
          (cachedValues(de).asInstanceOf[EvalResult[M]], this)
        case de: DerivedEval[M] => evaluateDerivedEval(de, oldValue)
        case be: BaseEval[M] => (evaluateBaseEval(be), this)
      }
      (res, contextAfterEval.copy(
        collectedDependencies = contextAfterEval.collectedDependencies.map(_ + eval)
      ))
    }

    private def evaluateDerivedEval[M](dm: DerivedEval[M], oldValue: Option[M]): (EvalResult[M], Impl) = {
      val oldDependencies = this.dependencyGraph.getDependencies(dm)
      val contextWithCollector = copy(collectedDependencies = Some(Set()))
      val (result, contextAfterEval) = dm.eval(contextWithCollector, oldValue)
      val newDependencies = contextAfterEval.asInstanceOf[Impl].collectedDependencies.get
      val finalContext =
        contextAfterEval.asInstanceOf[Impl].copy(collectedDependencies = this.collectedDependencies)
        .cache(dm, result)
        .setDependencies(dm, newDependencies)
        .dropDependencies(oldDependencies -- newDependencies)
      (result, finalContext)
    }

    override def update(update: InputUpdate): IncrementalContext = {
      val changedInputs = update.updateMappings.filter {
        case (k, v) => !inputValues.contains(k) || inputValues(k) != v
      }
      changedInputs.foldLeft(this) { case (c, (k, v)) => c.updateInput(k, v) }
    }

    private def updateInput(input: Input[_], value: Any): Impl = {
      copy(inputValues = inputValues.updated(input, value))
        .propagateChange(InputEval(input))
    }

    private def propagateChange(m: Eval[_]): Impl =
      dependencyGraph.getProvisions(m).foldLeft(this) {
        case (c, am) => c.reevaluateAndPropagateIfChanged(am)
      }

    private def reevaluateAndPropagateIfChanged(am: Eval[_]): Impl =
      cachedValues.get(am) match {
        case Some(v) =>
          val oldValue = v match {
            case Value(x) => Some(x)
            case _ => None
          }
          val (er, newContext) = copy(cachedValues = cachedValues - am).doEvaluate(am, oldValue)
          if (er == v) newContext
          else newContext.propagateChange(am)
        case None => this
      }

    private def cache(m: DerivedEval[_], er: EvalResult[_]) =
      copy(cachedValues = cachedValues.updated(m, er))

    private def dropDependencies(mm: Iterable[Eval[_]]): Impl = {
      val z = (dependencyGraph, Set[Eval[_]]())
      val (finalGraph, allDropped) = mm.foldLeft(z) { case ((g, dropped), m) =>
        val (updatedGraph, newDropped) = g.dropUnusedRecursively(m)
        (updatedGraph, dropped ++ newDropped)
      }
      copy(
        dependencyGraph = finalGraph,
        cachedValues = cachedValues -- allDropped
      )
    }

    private def setDependencies(m: Eval[_], set: Set[Eval[_]]) = {
      copy(dependencyGraph = dependencyGraph.setNodeDependencies(m, set))
    }

    private def evaluateBaseEval[M](bm: BaseEval[M]): EvalResult[M] = bm match {
      case InputEval(input) =>
        if (inputValues.keySet(input)) Value(inputValues(input).asInstanceOf[M])
        else InputRequest(Set(input))
      case Constant(c) => Value(c)
    }

  }

}