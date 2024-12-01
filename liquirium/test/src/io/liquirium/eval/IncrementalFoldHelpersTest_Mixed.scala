package io.liquirium.eval

import io.liquirium.eval.IncrementalFoldHelpers.{IncrementalEval, IncrementalSeqEval}
import io.liquirium.eval.helpers.EvalHelpers.inputEval
import io.liquirium.eval.helpers.{EvalTest, IncrementalIntListWithRootValue}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class IncrementalFoldHelpersTest_Mixed extends EvalTest {

  case class TestInput(n: Int) extends Input[IncrementalSeq[Int]]

  case class TestInputWithRoot(n: Int) extends Input[IncrementalIntListWithRootValue]

  case class PairTestInput(n: Int) extends Input[Seq[(Int, String)]]

  private val seqInput = TestInput(1)
  private val seqMetric = inputEval(seqInput)

  test("a simple fold can be defined conveniently") {
    val listWithRootInput = TestInputWithRoot(1)
    val listWithRootMetric = inputEval(listWithRootInput)
    val ll = IncrementalIntListWithRootValue.apply(10)(1, 2, 3)
    fakeInput(listWithRootInput, ll)
    val getStart: IncrementalIntListWithRootValue => Int = _.rootValue * 10
    val m = listWithRootMetric.foldIncremental(getStart) { case (sum, e) => e + sum }
    evaluate(m) shouldEqual Value(106)
  }

  test("collecting from an incremental sequence yields another incremental sequence with collected values") {
    fakeInput(seqInput, IncrementalSeq(1, 2, 3))
    val m = seqMetric.collectIncremental { case x if x % 2 == 1 => (x * x).toString }
    evaluate(m) shouldEqual Value(IncrementalSeq("1", "9"))
  }

  test("mapping an incremental sequence yields another incremental sequence with the mapped values") {
    fakeInput(seqInput, IncrementalSeq(1, 2, 3))
    val m = seqMetric.mapIncremental(_.toString)
    evaluate(m) shouldEqual Value(IncrementalSeq("1", "2", "3"))
  }

  test("filtering an incremental sequence yields another incremental sequence with the filtered values") {
    fakeInput(seqInput, IncrementalSeq(1, 2, 3, 4))
    val m = seqMetric.filterIncremental(_ % 2 == 0)
    evaluate(m) shouldEqual Value(IncrementalSeq(2, 4))
  }

  test("an incremental sequence can be grouped into an incremental map") {
    fakeInput(seqInput, IncrementalSeq(1, 2, 3, 4))
    val m = seqMetric.groupByIncremental(_ % 2)
    evaluate(m).get.mapValue shouldEqual Map(
      0 -> IncrementalSeq(2, 4),
      1 -> IncrementalSeq(1, 3),
    )
  }

  test("two incremental values can be aggregated together in a merge fold") {
    val inputA = TestInputWithRoot(1)
    val inputB = TestInputWithRoot(2)
    val metricA = inputEval(inputA)
    val metricB = inputEval(inputB)

    val llA = IncrementalIntListWithRootValue.apply(10)(1, 2, 3)
    val llB = IncrementalIntListWithRootValue.apply(20)(2, 3)
    fakeInput(inputA, llA)
    fakeInput(inputB, llB)

    val getStart: (IncrementalIntListWithRootValue, IncrementalIntListWithRootValue) => Int = _.rootValue + _.rootValue
    val m = metricA.mergeFoldIncremental(metricB)(getStart)(_ + _)(_ + 2 * _)
    evaluate(m) shouldEqual Value(10 + 20 + 6 + 10)
  }

}
