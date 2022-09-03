package io.liquirium.eval

import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest}
import io.liquirium.eval.helpers.EvalTest

class EvalTest_Flatten extends EvalTest {

  test("nested evals can be flattened") {
    val m = Constant(Constant(3))
    evaluate(Eval.flatten(m)) shouldEqual Value(3)
  }

  test("flattening the same eval twice yields two equal results") {
    val m = Constant(Constant(3))
    Eval.flatten(m) shouldEqual Eval.flatten(m)
  }

  test("input requests are forwarded") {
    val mi = fakeInputRequest(testEvalWithDefault[Int](1), input(1))
    val m = Constant(mi)
    evaluate(Eval.flatten(m)) shouldEqual inputRequest(input(1))
  }

  test("the context is passed to the inner eval and then returned") {
    val m = Eval.flatten(Constant(Constant(1)))
    trace(m).last shouldEqual Constant(1)
    trace(m).head shouldEqual m
  }

}
