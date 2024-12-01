package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.helpers.IncrementalIntListWithRootValue
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class IncrementalFoldTest_Basics extends BasicTest {

  private var accessCounter: Int = 0

  def increaseCounter(): Unit = {
    accessCounter = accessCounter + 1
  }

  def emptyList(baseValue: Int): IncrementalIntListWithRootValue =
    IncrementalIntListWithRootValue.empty(baseValue, increaseCounter)

  private def incrementalIntList(baseValue: Int)(increments: Int*) =
    increments.foldLeft(emptyList(baseValue))(_ inc _)

  case object SumFold extends IncrementalFold[Int, IncrementalIntListWithRootValue, Int] {

    override def startValue(baseValue: IncrementalIntListWithRootValue): Int = baseValue.rootValue

    override def step(oldValue: Int, lastIncrement: Int): Int = oldValue + lastIncrement

  }

  test("folding a list yields a fold state with the correct value that may depend on the root element") {
    SumFold.fold(incrementalIntList(7)()).value shouldEqual 7
    SumFold.fold(incrementalIntList(1)(1, 2, 3)).value shouldEqual 7
  }

  test("the fold state can be updated and yields the correct value") {
    val l1 = incrementalIntList(0)(1, 2, 3)
    val l2 = l1.inc(4)
    SumFold.fold(l1).update(l2).value shouldEqual 10
  }

  test("when updating the individual values are not accessed more often than necessary") {
    val l1 = incrementalIntList(0)(1, 2, 3)
    val l2 = l1.inc(4)
    val fs1 = SumFold.fold(l1)
    accessCounter = 0
    fs1.update(l2).value
    accessCounter shouldEqual 1
  }

  test("not all intermediate fold states are saved but only at updates") {
    val l1 = incrementalIntList(0)(1, 2)
    val l2Half = l1.inc(3)
    val l2Full = l2Half.inc(4)
    val l3 = l2Full.inc(5).inc(6)
    val fs1 = SumFold.fold(l1).update(l2Full).update(l3)
    accessCounter = 0
    fs1.update(l2Half.inc(10)).value shouldEqual 16
    accessCounter shouldEqual 2
  }

  test("updating with an unrelated value is possible") {
    val l1 = incrementalIntList(0)(1, 2, 3)
    val l2 = incrementalIntList(2)(5)
    SumFold.fold(l1).update(l2).value shouldEqual 7
  }

  test("a fold can be created conveniently with a method") {
    val sumFold = IncrementalFold[Int, IncrementalIntListWithRootValue, Int](_.rootValue)(_ + _)
    val l1 = incrementalIntList(10)(1, 2, 3)
    sumFold.fold(l1).value shouldEqual 16
  }

}
