package io.liquirium.eval

import io.liquirium.eval.helpers.EvalHelpers.{input, inputEval, inputRequest}
import io.liquirium.eval.helpers.EvalTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class EvalTest_Sequence extends EvalTest {

  test("a sequence of evals can be converted to an eval of a sequence") {
    val m1 = fakeEvalWithValue(1)
    val m2 = fakeEvalWithValue(2)
    val m3 = fakeEvalWithValue(3)
    evaluate(Eval.sequence(Seq(m1, m2, m3))) shouldEqual Value(Seq(1, 2, 3))
  }

  test("if evals in the sequence yield an input request the result is the combination of these requests") {
    val m1 = fakeInputRequest(testEvalWithDefault[Int](1), input(1))
    val m2 = fakeEvalWithValue(2)
    val m3 = fakeInputRequest(testEvalWithDefault[Int](3), input(3))
    evaluate(Eval.sequence(Seq(m1, m2, m3))) shouldEqual inputRequest(input(1), input(3))
  }

  test("two evals obtained by calls to sequence with the same parameters are equal") {
    val m1 = fakeEvalWithValue(1)
    val m2 = fakeEvalWithValue(2)
    Eval.sequence(List(m1, m2)) shouldEqual Eval.sequence(List(m1, m2))
  }

  test("the context is passed to all evals in a sequence before being returned") {
    val m = Eval.sequence(List(Constant(1), Constant(2)))
    trace(m) shouldEqual Seq(m, Constant(1), Constant(2))
  }

  test("the context is passed to all evals in a sequence even when there are input requests") {
    val m = Eval.sequence(List(Constant(1), inputEval(1), Constant(2)))
    trace(m) shouldEqual Seq(m, Constant(1), inputEval(1), Constant(2))
  }

}
