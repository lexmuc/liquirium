package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.helpers.EvalHelpers.{derivedEval, derivedEvalWithEvalCounter, input, inputEval, inputRequest}

import java.util.concurrent.atomic.AtomicInteger

class IncrementalContextTest extends BasicTest {

  implicit class ConvenientUpdate(c: UpdatableContext) {
    def update(pairs: (Input[_], Any)*): UpdatableContext = c.update(InputUpdate(pairs.toMap))
  }

  private var context = IncrementalContext()

  private def updateInputs(ii: (Input[_], Any)*): Unit = {
    context = context.update(InputUpdate(ii.toMap))
  }

  private def eval[M](m: Eval[M]): EvalResult[M] = {
    val (er, newContext) = context.evaluate(m)
    context = newContext.asInstanceOf[IncrementalContext]
    er
  }

  private def i(n: Int) = input(n)

  private def im(n: Int) = inputEval(n)

//  private def derivedEvalWithEvalCounter[M](baseEval: Eval[M]): (Eval[M], AtomicInteger) = {
//    val counter = new AtomicInteger(0)
//    val m = new DerivedEval[M] {
//      override def eval(context: Context, oldValue: Option[M]): (EvalResult[M], Context) = {
//        counter.getAndIncrement()
//        context.evaluate(baseEval)
//      }
//    }
//    (m, counter)
//  }

  test("it can evaluate constants") {
    context.apply(Constant(3)) shouldEqual Value(3)
  }

  test("the value of an input eval is taken from the input values") {
    updateInputs(i(1) -> 11, i(2) -> 22)
    context(inputEval(2)) shouldEqual Value(22)
  }

  test("old input values are not lost when updating several times") {
    updateInputs(i(1) -> 11, i(2) -> 21)
    updateInputs(i(2) -> 22)
    context(inputEval(1)) shouldEqual Value(11)
    context(inputEval(2)) shouldEqual Value(22)
  }

  test("it returns an input request when an input is not set") {
    updateInputs(i(1) -> 11)
    context(inputEval(input(2))) shouldEqual inputRequest(input(2))
  }

  test("simple derived evals are properly evaluated") {
    updateInputs(i(1) -> 11)
    val m1 = im(1).map(_ * 3)
    context(m1) shouldEqual Value(33)
  }

  test("nested derived evals are properly evaluated") {
    updateInputs(i(1) -> 11)
    val m1 = im(1).map(_ * 3).map(_ * 2)
    context(m1) shouldEqual Value(66)
  }

  test("evaluation of a derived eval can yield an input request") {
    val m1 = im(1).map(_ * 3)
    context(m1) shouldEqual inputRequest(i(1))
  }

  test("a derived eval is not evaluated again until the input changes but the result can still be obtained") {
    updateInputs(i(1) -> 11)
    val (dm, counter) = derivedEvalWithEvalCounter(im(1))
    eval(dm) shouldEqual Value(11)
    counter.get() shouldEqual 1
    eval(dm) shouldEqual Value(11)
    counter.get() shouldEqual 1
    updateInputs(i(1) -> 22)
    eval(dm) shouldEqual Value(22)
    counter.get() shouldEqual 2
  }

  test("input request results are cached just like other results") {
    val (dm, counter) = derivedEvalWithEvalCounter(im(1))
    eval(dm) shouldEqual InputRequest(Set(i(1)))
    counter.get() shouldEqual 1
    eval(dm) shouldEqual InputRequest(Set(i(1)))
    counter.get() shouldEqual 1
  }

  test("when an expected input is set, the respective eval can be evaluated") {
    val dm = derivedEval(im(1))
    eval(dm) shouldEqual InputRequest(Set(i(1)))
    updateInputs(i(1) -> 11)
    eval(dm) shouldEqual Value(11)
  }

  test("updating an input with the original value does not invalidate the cache") {
    updateInputs(i(1) -> 11)
    val (dm, counter) = derivedEvalWithEvalCounter(im(1))
    eval(dm) shouldEqual Value(11)
    counter.get() shouldEqual 1
    updateInputs(i(1) -> 11)
    eval(dm) shouldEqual Value(11)
    counter.get() shouldEqual 1
  }

  test("evals are transitively invalidated") {
    updateInputs(i(1) -> 11)
    val dm1 = derivedEval(inputEval(1))
    val dm2 = derivedEval(dm1)
    val dm3 = derivedEval(dm2)
    eval(dm3) shouldEqual Value(11)
    updateInputs(i(1) -> 22)
    eval(dm3) shouldEqual Value(22)
  }

  test("a change in an eval cannot make the eval itself obsolete") {
    // if it could, we should test that the respective value is removed from the cache.
    // In the past we had such a test
    // Technically this would only be possible if the change in a eval yielded by a flat map would lead to the
    // reevaluation of the map() part. So we test that this is not happening
    // If this is possible depends on how the evals are implemented (i.e. flatMap builds on map and flatten).
    val mi = derivedEval(inputEval(1))
    var evalCounter = 0
    val m = Constant(1).flatMap { _ =>
      evalCounter = evalCounter + 1
      mi
    }
    updateInputs(i(1) -> 1)
    eval(m)
    evalCounter shouldEqual 1
    updateInputs(i(1) -> 2)
    eval(m)
    evalCounter shouldEqual 1
  }

  test("a eval can depend on several inputs and is invalidated when either of them changes") {
    val m = Eval.map2(inputEval(1), inputEval(2))(_ + _)
    updateInputs(i(1) -> 11, i(2) -> 22)
    eval(m) shouldEqual Value(33)
    updateInputs(i(1) -> 22)
    eval(m) shouldEqual Value(44)
    updateInputs(i(2) -> 33)
    eval(m) shouldEqual Value(55)
  }

  test("old dependencies are discarded and don't affect the cache anymore"){
    val switchEval = inputEval(1).flatMap { n => if (n % 2 == 0) inputEval(2) else inputEval(3) }
    val (m, counter) = derivedEvalWithEvalCounter(switchEval)
    updateInputs(i(1) -> 2, i(2) -> 10)
    eval(m) shouldEqual Value(10)
    updateInputs(i(1) -> 3, i(3) -> 11)
    eval(m) shouldEqual Value(11)
    val countBefore = counter.get()
    updateInputs(i(2) -> 1000)
    eval(m) shouldEqual Value(11)
    counter.get() shouldEqual countBefore
  }

  test("evals that no other evals depend on anymore are removed from the cache") {
    val (dm, counter) = derivedEvalWithEvalCounter(inputEval(2))
    val switchEval = inputEval(1).flatMap { n =>
      if (n % 2 == 0) dm else inputEval(3)
    }
    updateInputs(i(1) -> 2, i(2) -> 22)
    eval(switchEval)
    updateInputs(i(1) -> 1)
    eval(switchEval)
    updateInputs(i(1) -> 2)
    eval(switchEval)
    counter.get() shouldEqual 2
  }

  test("irrelevant evals are transitively removed") {
    val (dm, counter) = derivedEvalWithEvalCounter(inputEval(2))
    val switchEval = inputEval(1).flatMap { n =>
      if (n % 2 == 0) derivedEval(dm) else inputEval(3)
    }
    updateInputs(i(1) -> 2, i(2) -> 22)
    eval(switchEval)
    updateInputs(i(1) -> 1)
    eval(switchEval)
    updateInputs(i(1) -> 2)
    eval(switchEval)
    counter.get() shouldEqual 2
  }

  test("evals are not removed from the cache when there are other dependencies") {
    updateInputs(i(2) -> 22)
    val (dm, counter) = derivedEvalWithEvalCounter(inputEval(2))
    eval(derivedEval(dm, 1))
    val switchEval = inputEval(1).flatMap { n =>
      if (n % 2 == 0) derivedEval(dm, 2) else inputEval(3)
    }
    updateInputs(i(1) -> 2)
    eval(switchEval)
    updateInputs(i(1) -> 1)
    eval(switchEval)
    updateInputs(i(1) -> 2)
    eval(switchEval)
    counter.get() shouldEqual 1
  }

  test("evals are removed from the cache when the last dependency is removed") {
    val (dm, counter) = derivedEvalWithEvalCounter(inputEval(2))
    val (dm1, dm2) = (derivedEval(dm, 1), derivedEval(dm, 2))
    val m = Eval.map2(dm1, dm2)(_ + _)
    val switchEval = inputEval(1).flatMap { n =>
      if (n % 2 == 0) m else inputEval(3)
    }
    updateInputs(i(1) -> 2)
    eval(switchEval)
    counter.get() shouldEqual 1
    updateInputs(i(1) -> 1)
    eval(switchEval)
    updateInputs(i(1) -> 2)
    eval(switchEval)
    counter.get() shouldEqual 2
  }

  test("evals are only evaluated again, when the value of one of their dependencies actually changes") {
    val modEval = inputEval(1).map(x => x % 2)
    val (dm, counter) = derivedEvalWithEvalCounter(modEval)
    updateInputs(i(1) -> 1)
    eval(dm)
    updateInputs(i(1) -> 3)
    eval(dm)
    counter.get() shouldEqual 1
    updateInputs(i(1) -> 2)
    eval(dm)
    counter.get() shouldEqual 2
  }

  test("new dependencies are still cached when a eval is reevaluated but doesn't change its value") {
    val (dm, counter) = derivedEvalWithEvalCounter(inputEval(2))
    val switchEval = inputEval(1).flatMap { n =>
      if (n % 2 == 0) dm else inputEval(3)
    }
    updateInputs(i(1) -> 1, i(2) -> 5, i(3) -> 5)
    eval(switchEval)
    counter.get() shouldEqual 0
    updateInputs(i(1) -> 2)
    counter.get() shouldEqual 1
    eval(dm)
    counter.get() shouldEqual 1
  }

  test("cached values further down the dependency chain can be reused even if intermediate evals change") {
    val (countEval, counter) = derivedEvalWithEvalCounter(inputEval(2))
    val (sub1, sub2) = (countEval.map(_ * 2).map(_ * 3), countEval.map(_ * 3).map(_ * 2))
    sub1 should not equal sub2
    val switchEval = inputEval(1).flatMap { n => if (n % 2 == 0) sub1 else sub2 }
    updateInputs(i(1) -> 1, i(2) -> 5)
    eval(switchEval)
    counter.get() shouldEqual 1
    updateInputs(i(1) -> 2)
    eval(switchEval)
    counter.get() shouldEqual 1
  }

  test("evals are supplied with the old value when they are reevaluated") {
    val im = inputEval(1)
    val probeEval = new DerivedEval[Int] {
      override def eval(context: Context, oldValue: Option[Int]): (EvalResult[Int], Context) =
        oldValue match {
          case Some(ov) => (Value(ov + 1), context)
          case None => context.evaluate(im)
        }
    }
    updateInputs(i(1) -> 10)
    eval(probeEval) shouldEqual Value(10)
    updateInputs(i(1) -> 1)
    eval(probeEval) shouldEqual Value(11)
  }

  test("evals are not supplied with any old value when they yielded an input request") {
    val im1 = inputEval(1)
    val probeEval = new DerivedEval[Int] {
      override def eval(context: Context, oldValue: Option[Int]): (EvalResult[Int], Context) =
        oldValue match {
          case Some(ov) => (Value(ov + 1), context)
          case None =>
            val (_, c) = context.evaluate(im1)
            (InputRequest(Set(input(2))), c)
        }
    }
    updateInputs(i(1) -> 1)
    eval(probeEval) shouldEqual InputRequest(Set(input(2)))
    updateInputs(i(1) -> 3)
    eval(probeEval) shouldEqual InputRequest(Set(input(2)))
  }

}
