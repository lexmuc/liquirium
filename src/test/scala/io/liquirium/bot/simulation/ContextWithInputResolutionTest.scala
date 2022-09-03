package io.liquirium.bot.simulation

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.helpers.ContextHelpers.inputUpdate
import io.liquirium.eval.helpers.EvalHelpers.{derivedEvalWithSideEffect, input, inputEval, inputRequest}
import io.liquirium.eval._

class ContextWithInputResolutionTest extends BasicTest {

  private var resolutions: Map[Input[_], Any] = Map()

  private def fakeResolution[I](i: Input[I], v: I): Unit = {
    resolutions = resolutions.updated(i, v)
  }

  private var baseContext = IncrementalContext()

  private def makeInitContext() = ContextWithInputResolution(baseContext, i => { resolutions.get(i) })

  private def fakeBaseContextInputValue[I](i: Input[I], v: I): Unit = {
    baseContext = baseContext.update(inputUpdate(i -> v))
  }

  test("evaluations are forwarded to the base context") {
    val e = inputEval(input(1))
    fakeBaseContextInputValue(input(1), 11)
    val (result, _) = makeInitContext().evaluate(e)
    result shouldEqual Value(11)
  }

  test("the state of the base context is maintained over evaluations") {
    var evalCounter = 0
    val e = derivedEvalWithSideEffect(Constant(1), () => {
      evalCounter = evalCounter + 1
    })
    val context0 = makeInitContext()
    val (_, context1) = context0.evaluate(e)
    evalCounter shouldEqual 1
    context1.evaluate(e)
    evalCounter shouldEqual 1
  }

  test("an input is provided via the given resolver if not set in the base context") {
    fakeResolution(input(1), 123)
    val e = inputEval(input(1))
    val (result, _) = makeInitContext().evaluate(e)
    result shouldEqual Value(123)
  }

  test("input values are not changed when already set on the base context") {
    fakeBaseContextInputValue(input(1), 111)
    fakeResolution(input(1), 0)
    val (result, _) = makeInitContext().evaluate(InputEval(input(1)))
    result shouldEqual Value(111)
  }

  test("the base context is updated after an input is provided") {
    var evalCounter = 0
    val e = derivedEvalWithSideEffect(InputEval(input(1)), () => {
      evalCounter = evalCounter + 1
    })
    fakeResolution(input(1), 0)
    val (_, context1) = makeInitContext().evaluate(e)
    evalCounter shouldEqual 2 // once for failed attempt and once when input is provided
    context1.evaluate(e)
    evalCounter shouldEqual 2
  }

  test("inputs that are not automatically provided are regularly forwarded in an input request") {
    val e = Eval.map2(inputEval(input(1)), inputEval(input(2)))(_ + _)
    val (result, _) = makeInitContext().evaluate(e)
    result shouldEqual inputRequest(input(1), input(2))
  }

  test("after forwarding input requests the base context is updated") {
    var evalCounter = 0
    val e = derivedEvalWithSideEffect(InputEval(input(1)), () => {
      evalCounter = evalCounter + 1
    })
    val (result, contextAfterEvaluation) = makeInitContext().evaluate(e)
    result shouldEqual inputRequest(input(1))
    evalCounter shouldEqual 1
    contextAfterEvaluation.evaluate(e)
    evalCounter shouldEqual 1
  }

  test("several inputs can be provided at once") {
    val e = Eval.map2(inputEval(input(1)), inputEval(input(2)))(_ + _)
    fakeResolution(input(1), 1)
    fakeResolution(input(2), 2)
    val (result, _) = makeInitContext().evaluate(e)
    result shouldEqual Value(3)
  }

  test("only unknown inputs are forwarded when combined with provided ones in one input request") {
    val e = Eval.map2(inputEval(input(1)), inputEval(input(2)))(_ + _)
    fakeResolution(input(1), 1)
    val (result, _) = makeInitContext().evaluate(e)
    result shouldEqual inputRequest(input(2))
  }

  test("in case of provided inputs base context caching is exploited when evaluating again (use last context)") {
    var evalCounter = 0
    val countEval = derivedEvalWithSideEffect(InputEval(input(1)), () => {
      evalCounter = evalCounter + 1
    })
    val e = Eval.map2(countEval, inputEval(input(2)))(_ + _)
    fakeBaseContextInputValue(input(1), 1)
    fakeResolution(input(2), 2)
    val (result, _) = makeInitContext().evaluate(e)
    result shouldEqual Value(3)
    evalCounter shouldEqual 1
  }

  test("when the provision of inputs yields a new input request inputs may be provided again") {
    fakeResolution(input(1), 1)
    fakeResolution(input(2), 22)
    val e = inputEval(input(1)).flatMap { x =>
      if (x == 1) inputEval(input(2)) else Constant(0)
    }
    val (result, _) = makeInitContext().evaluate(e)
    result shouldEqual Value(22)
  }

  test("input updates are forwarded to the base context") {
    val e = Eval.map2(inputEval(input(1)), inputEval(input(2)))(_ + _)
    fakeBaseContextInputValue(input(1), 1)
    val context = makeInitContext().update(inputUpdate(input(2) -> 2))
    val (result, _) = context.evaluate(e)
    result shouldEqual Value(3)
  }
  
}
