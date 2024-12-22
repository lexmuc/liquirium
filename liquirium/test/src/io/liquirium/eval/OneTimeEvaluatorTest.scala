package io.liquirium.eval

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.AsyncTestWithScheduler
import io.liquirium.eval.helpers.ContextHelpers.TraceContext
import io.liquirium.eval.helpers.EvalHelpers.{input, inputEval}
import io.liquirium.eval.helpers.{EvalHelpers, TestContextWithMockedEval}
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Failure

class OneTimeEvaluatorTest extends AsyncTestWithScheduler with TestWithMocks {

  private var evalResultsByInputs: Map[Map[Input[_], Any], EvalResult[Int]] = Map()
  private val inputProvider = mock(classOf[InputProvider])

  private val metric = EvalHelpers.testEval[Int](1)

  private def eval(): Future[(Int, UpdatableContext)] = {
    val context = TestContextWithMockedEval(Map(), (metric, inputs) => {
      metric shouldEqual metric
      evalResultsByInputs(inputs)
    })
    Await.ready(OneTimeEvaluator(inputProvider).apply(metric, context), 3.seconds)
  }

  private def trace(m: Eval[_]): Seq[Eval[_]] = {
    val evaluator = OneTimeEvaluator(inputProvider)
    Await.result(evaluator.apply(m, TraceContext(Map(), Seq())), 3.seconds)._2.asInstanceOf[TraceContext].trace
  }

  private def evalAndGetValue(): Int = {
    eval().value.get.get._1
  }

  private def evalAndGetContext(): UpdatableContext = {
    eval().value.get.get._2
  }

  private def fakeEvalResult(inputs: Map[Input[_], Any], result: EvalResult[Int]): Unit = {
    evalResultsByInputs = evalResultsByInputs.updated(inputs, result)
  }

  private def fakeInputValue[I](i: Input[I], v: I): Unit = {
    inputProvider.apply(i) returns Some(Future(v))
  }

  private def fakeNoInputValue[I](i: Input[I]): Unit = {
    inputProvider.apply(i) returns None
  }

  test("the result is immediately returned if no input is required") {
    fakeEvalResult(Map(), Value(333))
    evalAndGetValue() shouldEqual 333
  }

  test("when input requests are returned by the first evaluation, they are obtained from the input provider") {
    fakeEvalResult(Map(), InputRequest(Set(input(1), input(2))))
    fakeEvalResult(Map(input(1) -> 11, input(2) -> 22), Value(333))
    fakeInputValue(input(1), 11)
    fakeInputValue(input(2), 22)
    evalAndGetValue() shouldEqual 333
  }

  test("inputs can be obtained in several iterations") {
    fakeEvalResult(Map(), InputRequest(Set(input(1))))
    fakeEvalResult(Map(input(1) -> 11), InputRequest(Set(input(2))))
    fakeEvalResult(Map(input(1) -> 11, input(2) -> 22), Value(333))
    fakeInputValue(input(1), 11)
    fakeInputValue(input(2), 22)
    evalAndGetValue() shouldEqual 333
  }

  test("it returns the context after the evaluation together with the result") {
    fakeEvalResult(Map(), InputRequest(Set(input(1))))
    fakeEvalResult(Map(input(1) -> 11), InputRequest(Set(input(2))))
    fakeEvalResult(Map(input(1) -> 11, input(2) -> 22), Value(333))
    fakeInputValue(input(1), 11)
    fakeInputValue(input(2), 22)
    val ctx = evalAndGetContext()
    ctx.evaluate(metric)._1 shouldEqual Value(333)
  }

  test("the context is properly passed through all of the evaluations") {
    fakeInputValue(input(1), 11)
    trace(inputEval(1)) shouldEqual Seq(inputEval(1), inputEval(1))
  }

  test("it fails with an exception when an input value cannot be obtained") {
    fakeEvalResult(Map(), InputRequest(Set(input(1), input(2))))
    fakeInputValue(input(1), 11)
    fakeNoInputValue(input(2))
    eval().value shouldEqual Some(Failure(UnknownInputsException(Set(input(2)))))
  }

}
