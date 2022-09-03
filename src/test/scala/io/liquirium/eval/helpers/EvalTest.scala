package io.liquirium.eval.helpers

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.eval.helpers.ContextHelpers.TraceContext
import io.liquirium.eval.helpers.EvalHelpers.TestEval
import io.liquirium.eval._

import java.util.concurrent.atomic.AtomicInteger
import scala.reflect.ClassTag

class EvalTest extends TestWithMocks {

  protected var inputValues: Map[Input[_], Any] = Map()

  protected def fakeInput(i: Input[_], v: Any): Unit = {
    inputValues = inputValues.updated(i, v)
  }

  protected def trace(m: Eval[_]): Seq[Eval[_]] = {
    val traceContext = ContextHelpers.TraceContext(inputValues, Seq())
    val (_, finalContext) = traceContext.evaluate(m)
    finalContext.asInstanceOf[TraceContext].trace
  }

  val nextEvalIndex = new AtomicInteger(1)

  var resultsByEval: Map[Eval[_], EvalResult[_]] = Map()

  private def fakeResult(m: Eval[_], r: EvalResult[_]): Unit = {
    resultsByEval = resultsByEval.updated(m, r)
  }

  private val context = new Context {
    override def evaluate[M](eval: Eval[M]): (EvalResult[M], Context) = {
      val er = {
        if (resultsByEval.keySet(eval)) resultsByEval(eval).asInstanceOf[EvalResult[M]]
        else eval match {
          case m: DerivedEval[M] => m.eval(this, None)._1
          case Constant(c) => Value(c)
          case InputEval(i) if inputValues.contains(i) => Value(inputValues(i).asInstanceOf[M])
          case InputEval(i) => InputRequest(Set(i))
        }
      }
      (er, this)
    }
  }

  def testEval[M](): Eval[M] = TestEval[M](nextEvalIndex.getAndIncrement())

  def testEvalWithDefault[M](defaultValue: M): Eval[M] = {
    val m = TestEval[M](nextEvalIndex.getAndIncrement())
    fakeEvalValue(m, defaultValue)
    m
  }

  def fakeEvalValue[M](eval: Eval[M], value: M): Eval[M] = {
    fakeResult(eval, Value(value))
    eval
  }

  def fakeInputRequest[M](eval: Eval[M], inputs: Input[_]*): Eval[M] = {
    fakeResult(eval, InputRequest(inputs.toSet))
    eval
  }

  def evaluate[M](eval: Eval[M]): EvalResult[M] = context.apply(eval)

  def fakeEvalWithValue[M](value: M)(implicit tag: ClassTag[M]): Eval[M] = {
    val m = Constant(value)
    fakeResult(m, Value(value))
    m
  }

}
