package io.liquirium.eval


trait IncrementalContext extends UpdatableContext {

  override def update(update: InputUpdate): IncrementalContext

}

object IncrementalContext {

  def apply(): IncrementalContext = Impl(Map(), Map(), None, DependencyGraph.empty)

  private case class Impl(
    inputValues: Map[Input[_], _],
    cachedValues: Map[Eval[_], EvalResult[_]],
    parentEval: Option[Eval[_]],
    dependencyGraph: DependencyGraph[Eval[_]]
  ) extends IncrementalContext {

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], Impl) = internalEval(eval, None)

    private def internalEval[M](eval: Eval[M], oldValue: Option[M]): (EvalResult[M], Impl) = {
      val (er, newContext) = this.setParentEval(Some(eval)).doEvaluate(eval, oldValue)
      val resultContext = (parentEval match {
        case None => newContext
        case Some(pm) => newContext.addDependency(pm, eval)
      }).setParentEval(parentEval)
      (er, resultContext)
    }

    private def setParentEval(cm: Option[Eval[_]]) = copy(parentEval = cm)

    private def doEvaluate[M](eval: Eval[M], oldValue: Option[M]): (EvalResult[M], Impl) =
      eval match {
        case dm: DerivedEval[M] if cachedValues.contains(dm) =>
          (cachedValues(dm).asInstanceOf[EvalResult[M]], this)
        case dm: DerivedEval[M] =>
          val oldDependencies = this.dependencyGraph.getDependencies(dm)
          val (result, ctx1) = dm.eval(this.clearDependencies(dm), oldValue)
          val newDependencies = ctx1.asInstanceOf[Impl].dependencyGraph.getDependencies(dm)
          val ctx2 = ctx1.asInstanceOf[Impl]
            .cache(dm, result)
            .dropDependencies(oldDependencies -- newDependencies)
          (result, ctx2)
        case bm: BaseEval[M] => (evaluateBaseEval(bm), this)
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
          val (er, newContext) = copy(cachedValues = cachedValues - am).internalEval(am, oldValue)
          if (er == v) newContext
          else newContext.propagateChange(am)
        case None => this
      }

    private def cache(m: DerivedEval[_], er: EvalResult[_]) =
      copy(cachedValues = cachedValues.updated(m, er))

    private def clearDependencies(m: DerivedEval[_]): Impl =
      copy(dependencyGraph = dependencyGraph.removeDependenciesOf(m))

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

    private def addDependency(m: Eval[_], dep: Eval[_]) =
      copy(dependencyGraph = dependencyGraph.add(m, dep))

    private def evaluateBaseEval[M](bm: BaseEval[M]): EvalResult[M] = bm match {
      case InputEval(input) =>
        if (inputValues.keySet(input)) Value(inputValues(input).asInstanceOf[M])
        else InputRequest(Set(input))
      case Constant(c) => Value(c)
    }

  }

}