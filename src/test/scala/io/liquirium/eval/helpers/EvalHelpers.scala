package io.liquirium.eval.helpers

import io.liquirium.eval._
import org.scalatest.Matchers

import scala.reflect.ClassTag

object EvalHelpers extends Matchers {

  def constant[M](value: M)(implicit tag: ClassTag[M]): Constant[M] = Constant(value)(tag)

  case class TestEval[M](n: Int) extends DerivedEval[M] {
    override def eval(context: Context, oldValue: Option[M]): (EvalResult[M], Context) =
      throw new RuntimeException(s"Apparently no value has been provided for test eval $n")
  }

  def testEval[M](n: Int): Eval[Nothing] = TestEval(n)

  def intEval(n: Int): Eval[Int] = TestEval(n)

  def baseEval[M](x: M)(implicit tag: ClassTag[M]): BaseEval[M] = Constant(x)(tag)

  case class DerivedTestEval[M](subEval: Eval[M], id: Int) extends DerivedEval[M] {
    override def eval(context: Context, oldValue: Option[M]): (EvalResult[M], Context) = context.evaluate(subEval)
  }

  def derivedEval[M](subEval: Eval[M], n: Int = 0): DerivedTestEval[M] = DerivedTestEval(subEval, n)

  case class DerivedEvalWithSideEffect[M](subEval: Eval[M], sideEffect: () => Unit) extends DerivedEval[M] {
    override def eval(context: Context, oldValue: Option[M]): (EvalResult[M], Context) = {
      sideEffect()
      context.evaluate(subEval)
    }
  }

  def derivedEvalWithConversion[A, B](subEval: Eval[A], conversion: A => B): Eval[B] = subEval.map(conversion)

  def derivedEvalWithSideEffect[M](subEval: Eval[M], sideEffect: () => Unit): DerivedEvalWithSideEffect[M] =
    DerivedEvalWithSideEffect(subEval, sideEffect)

  case class TestInput(n: Int) extends Input[Int]

  def input(n: Int): Input[Int] = TestInput(n)

  def inputEval[M](i: Input[M]): InputEval[M] = InputEval(i)

  def inputEval(n: Int): InputEval[Int] = inputEval(input(n))

  def inputRequest(n: Int): InputRequest = InputRequest(Set(input(n)))

  def inputRequest(ii: Input[_]*): InputRequest = InputRequest(ii.toSet)

  def unknownInputsException(ii: Input[_]*): UnknownInputsException = UnknownInputsException(ii.toSet)

}
