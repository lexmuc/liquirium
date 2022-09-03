package io.liquirium.eval

import io.liquirium.eval.helpers.EvalHelpers.{input, inputEval, inputRequest}
import io.liquirium.eval.helpers.EvalTest

class EvalTest_FlatMap extends EvalTest {

  test("flat-mapping an eval is possible") {
    val m1 = fakeEvalValue(testEval[Int](), 123)
    val m2 = fakeEvalValue(testEval[String](), "234")
    evaluate(m1.flatMap(Map(123 -> m2))) shouldEqual Value("234")
  }

  test("flat-mapping a eval internally relies on a mapped eval that produces the map result") {
    val mapFunction: Int => Eval[Int] = x => Constant(x * x)
    val m1 = testEval[Int]()
    fakeEvalValue(m1.map(mapFunction), Constant(7))
    evaluate(m1.flatMap(mapFunction)) shouldEqual Value(7)
  }

  test("during a flatMap the context evaluates the given eval and the returned eval before being returned") {
    val m = Constant(2).flatMap(n => Constant(n * 3))
    trace(m).takeRight(2) shouldEqual Seq(Constant(2), Constant(6))
    trace(m).head shouldEqual m
  }

  test("the context is properly passed around during a flatMap yielding an input request") {
    val m = inputEval(1).flatMap(_ => inputEval(2))
    trace(m).last shouldEqual inputEval(1)
    fakeInput(input(1), 1)
    trace(m).takeRight(2) shouldEqual Seq(inputEval(1), inputEval(2))
  }

  test("two evals derived from the same base eval with the same function are equal") {
    val baseEval = testEvalWithDefault(123)
    val f = (i: Int) => testEvalWithDefault(1000 + i)
    baseEval.flatMap(f) shouldEqual baseEval.flatMap(f)
  }

  test("input requests are also forwarded for both eval in a flatMap") {
    val mi = fakeInputRequest(testEvalWithDefault[Int](1), input(1))
    val mv = fakeEvalValue(testEval[Int](), 123)
    evaluate(mi.flatMap(Map(123 -> mv))) shouldEqual inputRequest(input(1))
    evaluate(mv.flatMap(Map(123 -> mi))) shouldEqual inputRequest(input(1))
  }

}
