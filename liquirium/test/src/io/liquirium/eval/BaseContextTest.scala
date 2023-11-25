package io.liquirium.eval

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.eval.helpers.EvalHelpers.{input, inputEval, inputRequest}

class BaseContextTest extends TestWithMocks {

  private var inputValues = Map[Input[_], Any]()

  private def basicContext = BaseContext(inputValues)

  private def eval[M](m: Eval[M]) = basicContext(m)

  test("the empty base context can evaluate constants") {
    BaseContext.empty.apply(Constant(3)) shouldEqual Value(3)
  }

  test("the value of an input eval is taken from the input values") {
    inputValues = Map(input(1) -> 11, input(2) -> 22)
    eval(inputEval(2)) shouldEqual Value(22)
  }

  test("it returns an input request when an input is not set") {
    inputValues = Map(input(1) -> 11)
    eval(inputEval(input(2))) shouldEqual inputRequest(input(2))
  }

  test("the result of derived eval evaluation is returned regardless of whether it is a value or input request") {
    val valueEval = new DerivedEval[Int] {
      override def eval(context: Context, oldValue: Option[Int]): (EvalResult[Int], Context) =
        (Value(123), context)
    }
    eval(valueEval) shouldEqual Value(123)

    val inputRequestEval = new DerivedEval[Int] {
      override def eval(context: Context, oldValue: Option[Int]): (EvalResult[Int], Context) =
        (inputRequest(33), context)
    }
    eval(inputRequestEval) shouldEqual inputRequest(33)
  }

  test("a BaseContext created from a set of input values contains exactly those inputs") {
    val inputs = Map[Input[_], Any](
      input(1) -> 11,
      input(2) -> 22
    )
    val bc = BaseContext.fromInputValues(inputs)
    bc.inputValues shouldEqual inputs
  }

  test("an input update replaces or adds input values for the respective keys") {
    val c1 = basicContext
      .update(InputUpdate(Map(
        input(1) -> 11,
        input(2) -> 22
      )))
    c1(inputEval(1)) shouldEqual Value(11)
    val c2 = c1
      .update(InputUpdate(Map(
        input(1) -> 111,
        input(3) -> 3
      )))
    c2(inputEval(1)) shouldEqual Value(111)
    c2(inputEval(2)) shouldEqual Value(22)
    c2(inputEval(3)) shouldEqual Value(3)
  }

}
