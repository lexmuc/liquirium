package io.liquirium.eval

import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest}
import io.liquirium.eval.helpers.EvalTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class EvalTest_Map extends EvalTest {

  test("mapping an eval yields a new eval transforming the value with the given function") {
    val m1 = fakeEvalValue(testEvalWithDefault[Int](1), 123)
    evaluate(m1.map(v => (v * 2).toString)) shouldEqual Value("246")
  }

  test("two evals created by mapping the same base eval with the same function are equal") {
    val baseEval = testEvalWithDefault(123)
    val f = (i: Int) => i + 1
    baseEval.map(f) shouldEqual baseEval.map(f)
  }

  test("input requests are forwarded when mapping") {
    val m1 = fakeInputRequest(testEvalWithDefault[Int](1), input(3))
    evaluate(m1.map(v => (v * 2).toString)) shouldEqual inputRequest(input(3))
  }

  test("the context is properly passed to the base eval and back during evaluation") {
    val baseEval = Constant(1)
    val mappedEval = baseEval.map(_ + 1)
    trace(mappedEval) shouldEqual Seq(mappedEval, Constant(1))
  }

}
