package io.liquirium.eval

import io.liquirium.eval.helpers.EvalTest

class EvalTest_Mixed extends EvalTest {

  test("a constant eval only equals another constant if the type matches, too") {
    Constant(1) shouldEqual Constant(1)
    Constant(1) should not equal Constant(2)
    Constant(1) should not equal Constant(BigDecimal(1))
    Constant(1) should not equal Constant(1f)
  }

  test("the type is taken into account when a constant eval is part of a set") {
    Set(Constant(1))(Constant(1)) shouldBe true
    Set[Any](Constant(1))(Constant(1f)) shouldBe false
  }

  test("a case eval only wraps a derived eval in a case class") {
    val base = testEvalWithDefault(17)
    case class TestCaseEval(factor: Int) extends CaseEval[Int] {
      override def baseEval: Eval[Int] = base.map(_ * 3)
    }
    evaluate(TestCaseEval(3)).get shouldEqual 51
  }

  test("the context is properly conveyed in a case eval") {
    val base = Constant(17)
    case class TestCaseEval(factor: Int) extends CaseEval[Int] {
      override def baseEval: Eval[Int] = base.map(_ * 3)
    }
    trace(TestCaseEval(3)).head shouldEqual TestCaseEval(3)
    trace(TestCaseEval(3)).last shouldEqual Constant(17)
  }

  test("Eval.unit is synonym for Constant") {
    Eval.unit(7) shouldEqual Constant(7)
  }

  test("only a mark eval carries a mark") {
    val c = Constant(7)
    c.getMark shouldEqual None
    c.map(_ + 1).getMark shouldEqual None
    c.mark("x").getMark shouldEqual Some("x")
  }

}
