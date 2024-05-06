package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.helpers.EvalHelpers.{derivedEval, derivedEvalWithEvalCounter, input, inputEval, inputRequest}

class ExplicitCacheContextTest extends BasicTest {

  private var context = ExplicitCacheContext()

  private def updateInputs(ii: (Input[_], Any)*): Unit = {
    context = context.update(InputUpdate(ii.toMap))
  }

  private def i(n: Int) = input(n)

  private def ie(n: Int) = inputEval(n)

  private def evaluate[M](m: Eval[M]): EvalResult[M] = {
    val (er, newContext) = context.evaluate(m)
    context = newContext.asInstanceOf[ExplicitCacheContext]
    er
  }

  test("it can evaluate constants") {
    context.apply(Constant(3)) shouldEqual Value(3)
  }

  test("the value of an input eval is taken from the input values") {
    updateInputs(i(1) -> 11)
    updateInputs(i(2) -> 22)
    context(inputEval(2)) shouldEqual Value(22)
  }

  test("inputs can be updated and other inputs are not affected") {
    updateInputs(i(1) -> 11)
    updateInputs(i(2) -> 22)
    updateInputs(i(1) -> 12)
    context(inputEval(1)) shouldEqual Value(12)
    context(inputEval(2)) shouldEqual Value(22)
  }

  test("several inputs can be updated at once") {
    updateInputs(i(1) -> 11)
    updateInputs(i(2) -> 22)
    val eval = Eval.map2(ie(1), ie(2))(_ + _)
    context(eval) shouldEqual Value(33)
  }

  test("it returns an input request when an input is not set") {
    updateInputs(i(1) -> 11)
    context(inputEval(input(2))) shouldEqual inputRequest(input(2))
  }

  test("simple derived evals are properly evaluated") {
    updateInputs(i(1) -> 11)
    val m1 = ie(1).map(_ * 3)
    context(m1) shouldEqual Value(33)
  }

  test("nested derived evals are properly evaluated") {
    updateInputs(i(1) -> 11)
    val m1 = ie(1).map(_ * 3).map(_ * 2)
    context(m1) shouldEqual Value(66)
  }

  test("evaluation of a derived eval can yield an input request") {
    val m1 = ie(1).map(_ * 3)
    context(m1) shouldEqual inputRequest(i(1))
  }

  test("a cached eval is cached and not evaluated again if no input has changed") {
    updateInputs(i(1) -> 11)
    val (eval, counter) = derivedEvalWithEvalCounter(ie(1))
    val cachedEval = eval.cached
    evaluate(cachedEval) shouldEqual Value(11)
    counter.get() shouldEqual 1
    evaluate(cachedEval) shouldEqual Value(11)
    counter.get() shouldEqual 1
  }

  test("a cached eval is cached when it is part of an evaluated expression") {
    updateInputs(i(1) -> 11)
    val (counterEval, counter) = derivedEvalWithEvalCounter(ie(1))
    val eval = counterEval.cached.map(_ + 1)
    evaluate(eval) shouldEqual Value(12)
    counter.get() shouldEqual 1
    evaluate(eval) shouldEqual Value(12)
    counter.get() shouldEqual 1
  }

  test("a cached eval is evaluated again when an input it depends on changes") {
    updateInputs(i(1) -> 11)
    updateInputs(i(2) -> 22)
    val cachedEval = Eval.map2(ie(1), ie(2))(_ + _).cached
    evaluate(cachedEval) shouldEqual Value(33)
    updateInputs(i(1) -> 12)
    evaluate(cachedEval) shouldEqual Value(34)
  }

  test("a cached eval is not evaluated again when an independent input changes") {
    updateInputs(i(1) -> 11)
    updateInputs(i(2) -> 22)
    val (counterEval, counter) = derivedEvalWithEvalCounter(ie(1))
    val cachedEval = counterEval.cached
    evaluate(cachedEval) shouldEqual Value(11)
    updateInputs(i(2) -> 23)
    evaluate(cachedEval) shouldEqual Value(11)
    counter.get() shouldEqual 1
  }

  test("input requests are cached as well and the cache is invalidated when the respective input is updated") {
    val (counterEval, counter) = derivedEvalWithEvalCounter(ie(1).map(_ * 2))
    val cachedEval = counterEval.cached
    evaluate(cachedEval) shouldEqual inputRequest(i(1))
    evaluate(cachedEval) shouldEqual inputRequest(i(1))
    counter.get() shouldEqual 1
    updateInputs(i(1) -> 11)
    evaluate(cachedEval) shouldEqual Value(22)
  }

  test("several cached evals are properly managed with their dependencies (dependency collection is independent)") {
    val (counterEvalA, counterA) = derivedEvalWithEvalCounter(ie(1).map(_ * 2))
    val (counterEvalB, counterB) = derivedEvalWithEvalCounter(ie(2).map(_ * 2))
    val cachedEval = Eval.map2(counterEvalA.cached, counterEvalB.cached)(_ + _)

    updateInputs(i(1) -> 1)
    updateInputs(i(2) -> 2)
    evaluate(cachedEval) shouldEqual Value(6)
    counterA.get() shouldEqual 1
    counterB.get() shouldEqual 1

    updateInputs(i(1) -> 2)
    evaluate(cachedEval) shouldEqual Value(8)
    counterA.get() shouldEqual 2
    counterB.get() shouldEqual 1

    updateInputs(i(2) -> 1)
    evaluate(cachedEval) shouldEqual Value(6)
    counterA.get() shouldEqual 2
    counterB.get() shouldEqual 2
  }

  test("a cached eval is only reevaluated once if several dependencies change") {
    val sumEval = Eval.map2(ie(1), ie(2))(_ + _)
    val (counterEval, counter) = derivedEvalWithEvalCounter(sumEval)
    val cachedEval = counterEval.cached

    updateInputs(i(1) -> 1)
    updateInputs(i(2) -> 2)
    evaluate(cachedEval) shouldEqual Value(3)
    counter.get() shouldEqual 1

    updateInputs(i(1) -> 2)
    updateInputs(i(2) -> 4)
    evaluate(cachedEval) shouldEqual Value(6)
    counter.get() shouldEqual 2
  }

  test("a cached eval may depend on another cached eval and both are reevaluated if required") {
    val innerCachedEval = ie(1).map(_ * 2).cached
    val outerCachedEval = innerCachedEval.map(_ + 1).cached
    updateInputs(i(1) -> 2)
    evaluate(outerCachedEval) shouldEqual Value(5)

    updateInputs(i(1) -> 3)
    evaluate(outerCachedEval) shouldEqual Value(7)
  }

  test("dependencies are properly managed across several levels") {
    val level1EvalA = Eval.map2(ie(1), ie(2))(_ + _).cached
    val level1EvalB = Eval.map2(ie(3), ie(4))(_ + _).cached
    val level2Eval = Eval.map2(level1EvalA, level1EvalB)(_ + _).cached
    updateInputs(i(1) -> 1)
    updateInputs(i(2) -> 2)
    updateInputs(i(3) -> 3)
    updateInputs(i(4) -> 4)

    evaluate(level2Eval) shouldEqual Value(10)
    updateInputs(i(1) -> 2)
    evaluate(level2Eval) shouldEqual Value(11)
    updateInputs(i(4) -> 5)
    evaluate(level2Eval) shouldEqual Value(12)
  }

  test("an already cached eval may be added to the dependencies of a another eval") {
    val existingCachedEval = ie(1).map(_ * 2).cached
    updateInputs(i(1) -> 1)
    evaluate(existingCachedEval) shouldEqual Value(2)
    val newCachedEval = existingCachedEval.map(_ * 7).cached
    evaluate(newCachedEval) shouldEqual Value(14)
    updateInputs(i(1) -> 2)
    evaluate(newCachedEval) shouldEqual Value(28)
  }

  test("nested cached values are actually cached") {
    val (branch1, branch1Counter) = derivedEvalWithEvalCounter(ie(1).map(_ * 2))
    val (branch2, branch2Counter) = derivedEvalWithEvalCounter(ie(2).map(_ * 2))
    val eval = Eval.map2(branch1.cached.cached, branch2.cached.cached)(_ + _).cached
    updateInputs(i(1) -> 1)
    updateInputs(i(2) -> 2)
    evaluate(eval) shouldEqual Value(6)
    branch2Counter.get() shouldEqual 1

    updateInputs(i(1) -> 2)
    evaluate(eval) shouldEqual Value(8)
    branch1Counter.get() shouldEqual 2
    branch2Counter.get() shouldEqual 1
  }

//  test("a cached eval is not evaluated when a cached eval it depends on has not changed after an update") {
//    val (innerCounterEval, innerCounter) = derivedEvalWithEvalCounter(ie(1).map(_ % 2))
//    val innerCachedEval = innerCounterEval.cached
//    val (outerCounterEval, outerCounter) = derivedEvalWithEvalCounter(innerCachedEval.map(_ + 1))
//    val outerCachedEval = outerCounterEval.cached
//
//    updateInputs(i(1) -> 2)
//    evaluate(outerCachedEval) shouldEqual Value(1)
//
//    outerCounter.set(0)
//    innerCounter.set(0)
//    updateInputs(i(1) -> 4)
//    evaluate(outerCachedEval) shouldEqual Value(1)
//    outerCounter.get() shouldEqual 0
//    innerCounter.get() shouldEqual 1
//  }

}
