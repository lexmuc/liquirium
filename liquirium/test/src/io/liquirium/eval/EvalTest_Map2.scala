package io.liquirium.eval

import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest}
import io.liquirium.eval.helpers.EvalTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class EvalTest_Map2 extends EvalTest {

  test("map2 yields a derived eval combining the values with the given function") {
    val m1 = fakeEvalValue(testEvalWithDefault[Int](1), 123)
    val m2 = fakeEvalValue(testEval[String](), "234")
    evaluate(Eval.map2(m1, m2) { case (i, s) => i.toString + "|" + s}) shouldEqual Value("123|234")
  }

  test("two evals obtained by a call to map2 with the same parameters are equal") {
    val m1 = fakeEvalValue(testEvalWithDefault[Int](1), 123)
    val m2 = fakeEvalValue(testEvalWithDefault[Int](2), 123)
    val plus = (a: Int, b: Int) => a + b
    Eval.map2(m1, m2)(plus) shouldEqual Eval.map2(m1, m2)(plus)
  }

  test("evaluating the same mapped eval twice yields the same trace (same internally used evals if any)") {
    val m = Eval.map2(Constant(1), Constant(2))(_ + _)
    trace(m) shouldEqual trace(m)
  }

  test("all input requests are forwarded for map2 (input lists are combined in case of two requests)") {
    val mi12 = fakeInputRequest(testEvalWithDefault[Int](1), input(1), input(2))
    val mi23 = fakeInputRequest(testEvalWithDefault[Int](2), input(2), input(3))
    val mx = fakeEvalValue(testEvalWithDefault[Int](3), 3)
    evaluate(Eval.map2(mi12, mx)((_, _) => 1)) shouldEqual inputRequest(input(1), input(2))
    evaluate(Eval.map2(mx, mi12)((_, _) => 1)) shouldEqual inputRequest(input(1), input(2))
    evaluate(Eval.map2(mi12, mi23)((_, _) => 1)) shouldEqual inputRequest(input(1), input(2), input(3))
  }

  test("the context is passed to both map2 evals and then returned") {
    val m = Eval.map2(Constant(1), Constant(2))(_ + _)
    trace(m).takeRight(2) shouldEqual Seq(Constant(1), Constant(2))
    trace(m).head shouldEqual m
  }

}
