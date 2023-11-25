package io.liquirium.eval.helpers

import io.liquirium.eval._
import io.liquirium.eval.helpers.EvalHelpers.input

object ContextHelpers {

  type InputValues = Map[Input[_], _]

  case class InevaluableContext(n: Int) extends UpdatableContext {
    override def evaluate[M](eval: Eval[M]): (EvalResult[M], UpdatableContext) =
      throw new RuntimeException("Can't evaluate inevaluable context.")

    override def update(update: InputUpdate): UpdatableContext =
      throw new RuntimeException("Can't update inevaluable context.")

  }

  case class TraceContext(inputs: Map[Input[_], Any], trace: Seq[Eval[_]]) extends UpdatableContext {

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], UpdatableContext) = {
      eval match {
        case bm: BaseEval[M] =>
          (evaluateBaseEval(bm), this.appendToTrace(eval))
        case derivedEval: DerivedEval[M] =>
          val (res, ctx) = derivedEval.eval(this.appendToTrace(eval), None)
          (res, ctx.asInstanceOf[UpdatableContext])
      }
    }

    def evaluateBaseEval[M](bm: BaseEval[M]): EvalResult[M] = bm match {
      case Constant(c) => Value(c)
      case InputEval(i) if inputs.contains(i) => Value(inputs(i).asInstanceOf[M])
      case InputEval(i) => InputRequest(Set(i))
    }

    private def appendToTrace(m: Eval[_]) = copy(trace = trace :+ m)

    override def update(update: InputUpdate): UpdatableContext = copy(inputs = inputs ++ update.updateMappings)

  }

  case class FakeContextWithInputs(inputValues: InputValues) extends UpdatableContext {

    override def update(update: InputUpdate): FakeContextWithInputs = copy(inputValues ++ update.updateMappings)

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], FakeContextWithInputs) = ???

  }

  case class UpdateRecordingContext(reverseUpdates: List[InputUpdate]) extends UpdatableContext {

    override def update(update: InputUpdate): UpdateRecordingContext = copy(reverseUpdates = update :: reverseUpdates)

    override def evaluate[M](eval: Eval[M]): (EvalResult[M], UpdatableContext) =
      throw new RuntimeException("Can't do that.")

  }

  val updateRecordingContext: UpdateRecordingContext = UpdateRecordingContext(List())

  def context(n: Int): UpdatableContext = InevaluableContext(n)

  def simpleFakeContext(mappings: (Any, Any)*): SimpleFakeContext = {
    val evalMappings = mappings.map {
      case (e: Eval[_], v) => (e, Value(v))
      case (input: Input[_], v) => (InputEval(input), Value(v))
    }
    SimpleFakeContext(evalMappings.toMap)
  }

  def fakeContextWithInputs(keyValuePairs: (Input[_], _)*) : FakeContextWithInputs =
    FakeContextWithInputs(keyValuePairs.toMap)

  def inputUpdate[M](input: Input[M], value: M): InputUpdate = InputUpdate(Map(input -> value))

  def inputUpdate(pairs: (Input[_], _)*): InputUpdate = InputUpdate(pairs.toMap)

  def inputUpdate(n: Int): InputUpdate = InputUpdate(Map(input(n) -> n))

  def context(mappings: (Eval[_], Any)*): Context = new Context {
    override def evaluate[M](eval: Eval[M]): (EvalResult[M], Context) = {
      val er = mappings.find(_._1 == eval) match {
        case Some((_, v)) => Value(v.asInstanceOf[M])
        case _ => throw new RuntimeException("no value defined for " + eval)
      }
      (er, this)
    }
  }

  def sideEffectContext(baseContext: UpdatableContext, sideEffect: () => Unit): UpdatableContext =
    new UpdatableContext {
      private var base = baseContext

      override def update(update: InputUpdate): UpdatableContext = {
        base = base.update(update)
        this
      }

      override def evaluate[M](eval: Eval[M]): (EvalResult[M], UpdatableContext) = {
        sideEffect()
        (base(eval), this)
      }
    }

}
