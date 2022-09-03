package io.liquirium.eval

import io.liquirium.eval.helpers.EvalHelpers.inputEval
import io.liquirium.eval.helpers.EvalTest

class IncrementalFoldEvalTest extends EvalTest {

  private var accessCounter: Int = 0

  trait WrappedInt {
    def get: Int
  }

  case class WrappedIntImpl(n: Int) extends WrappedInt {
    override def get: Int = {
      accessCounter = accessCounter + 1
      n
    }
  }

  case class TestInput(n: Int) extends Input[IncrementalSeq[WrappedInt]]

  private val listInput = TestInput(1)
  private val baseMetric = inputEval(listInput)

  private val sumFold = new IncrementalFold[WrappedInt, IncrementalSeq[WrappedInt], Int] {
    override def startValue(baseValue: IncrementalSeq[WrappedInt]): Int = 0

    override def step(oldValue: Int, nextIncrement: WrappedInt): Int = oldValue + nextIncrement.get
  }

  private val foldMetric = IncrementalFoldEval(baseMetric, sumFold)

  private def seq(ints: Int*) = IncrementalSeq.from(ints.map(WrappedIntImpl.apply))

  test("it evaluates the given metric and applies the fold") {
    val c0 = IncrementalContext().update(InputUpdate(Map(listInput -> seq(3, 2, 1))))
    val (er, _) = c0.evaluate(foldMetric)
    er shouldEqual Value(6)
  }

  test("input requests are forwarded") {
    val c0 = IncrementalContext()
    val (er, _) = c0.evaluate(foldMetric)
    er shouldEqual InputRequest(Set(listInput))
  }

  test("the context after the evaluation is returned") {
    trace(foldMetric).head shouldEqual foldMetric
    trace(foldMetric) should contain(baseMetric)
  }

  test("it takes advantage of the base metric being incremental and only folds new elements") {
    val seq0 = seq(1, 2, 3)
    val c0 = IncrementalContext().update(InputUpdate(Map(listInput -> seq0)))
    val (_, c1) = c0.evaluate(foldMetric)
    val seq1 = seq0.inc(WrappedIntImpl(4))
    accessCounter = 0
    val c2 = c1.update(InputUpdate(Map(listInput -> seq1)))
    val (er, _) = c2.evaluate(foldMetric)
    er shouldEqual Value(10)
    accessCounter shouldEqual 1
  }

}
