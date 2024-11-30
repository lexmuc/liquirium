package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.helpers.IncrementalIntListWithRootValue
import org.scalatest.matchers.must.Matchers.{be, not}
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}

class IncrementalFoldTest_StateAsIncrementalValue extends BasicTest {

  private val sumFold = IncrementalFold[Int, IncrementalIntListWithRootValue, Int](_.rootValue)(_ + _)

  private def ints(root: Int)(nn: Int*) = IncrementalIntListWithRootValue(root)(nn: _*)

  test("the fold state itself is an incremental value") {
    sumFold.fold(ints(0)()) shouldBe an[IncrementalValue[_, _]]
  }

  test("the state provides the same state as root as long as base values are related") {
    val ii0 = ints(0)()
    val rootState = sumFold.fold(ii0)
    rootState.root should be theSameInstanceAs rootState
    rootState.update(ii0.inc(1)).root should be theSameInstanceAs rootState
    rootState.update(ii0.inc(1)).update(ii0).root should be theSameInstanceAs rootState
  }

  test("the root state may change when updating with an unrelated value") {
    val ii0 = ints(0)()
    val ii1 = ints(1)()
    val rootState = sumFold.fold(ii0)
    rootState.update(ii0.inc(1)).update(ii1).root should not equal rootState
  }

  test("the root state is equal regardless of whether the fold happened step by step or at once") {
    val ii0 = ints(0)()
    val ii1 = ii0.inc(1)
    val ii2 = ii1.inc(2)
    sumFold.fold(ii2).root shouldEqual sumFold.fold(ii0).update(ii1).update(ii2).root
  }

  test("the latest common ancestor is found if present") {
    val ii0 = ints(0)()
    val ii1 = ii0.inc(1)
    val ii2 = ii1.inc(2)
    val rootState = sumFold.fold(ii0)
    rootState.latestCommonAncestor(rootState).get should be theSameInstanceAs rootState
    rootState.update(ii2).latestCommonAncestor(rootState).get should be theSameInstanceAs rootState
    val state1 = rootState.update(ii1)
    state1.update(ii2).latestCommonAncestor(state1).get should be theSameInstanceAs state1
    val state2 = state1.update(ii2)
    state2.latestCommonAncestor(state2).get should be theSameInstanceAs state2

    rootState.latestCommonAncestor(sumFold.fold(ints(1)())) shouldBe None
    state2.latestCommonAncestor(sumFold.fold(ints(1)(2, 3))) shouldBe None
  }

  test("latest common ancestor works in both directions if one base value is an ancestor of the other") {
    val ii1 = ints(0)(1)
    val ii2 = ii1.inc(2)
    val ii3 = ii2.inc(3)
    val fs0 = sumFold.fold(ii1)
    val fs2 = fs0.update(ii2)
    val fs3 = fs0.update(ii3)
    fs2.latestCommonAncestor(fs3).get should be theSameInstanceAs fs2
    fs3.latestCommonAncestor(fs2).get should be theSameInstanceAs fs2
  }

  test("finding the latest common ancestor works even if the latest common ancestor of the base values differs") {
    val ii1 = ints(0)(1)
    val ii2 = ii1.inc(2)
    val ii3A = ii2.inc(3)
    val ii3B = ii2.inc(4)
    val fs0 = sumFold.fold(ii1)
    val fs3A = fs0.update(ii3A)
    val fs3B = fs0.update(ii3B)
    fs3A.latestCommonAncestor(fs3B).get should be theSameInstanceAs fs0
  }

  test("it can be tested if one state is an ancestor of the other") {
    val ii0 = ints(0)()
    val ii1 = ii0.inc(1)
    val ii2 = ii1.inc(2)
    val rootState = sumFold.fold(ii0)
    rootState.isAncestorOf(rootState) shouldBe true
    rootState.isAncestorOf(sumFold.fold(ints(1)())) shouldBe false
    rootState.update(ii2).isAncestorOf(rootState) shouldBe false
    rootState.isAncestorOf(rootState.update(ii2)) shouldBe true

    val state1 = rootState.update(ii1)
    state1.isAncestorOf(state1) shouldBe true
    state1.isAncestorOf(state1.update(ii2)) shouldBe true
    state1.update(ii2).isAncestorOf(state1) shouldBe false
  }

  test("the increments after another state can be obtained when it is an ancestor") {
    val r = ints(0)(1)
    val ii1 = r.inc(1)
    val ii2 = ii1.inc(2)
    val fs0 = sumFold.fold(r)
    val fs1 = fs0.update(ii1)
    val fs2 = fs0.update(ii2)
    fs0.incrementsAfter(fs0) shouldEqual List()
    fs1.incrementsAfter(fs0) shouldEqual List(1)
    fs2.incrementsAfter(fs0) shouldEqual List(1, 2)
    fs2.incrementsAfter(fs1) shouldEqual List(2)
  }

  test("an exception is thrown when trying to obtain increments from a non-ancestor state") {
    val r = ints(0)(1)
    val ii1 = r.inc(1)
    val ii2 = ii1.inc(2)
    val fs0 = sumFold.fold(r)
    val fs1 = fs0.update(ii1)
    val fs2 = fs0.update(ii2)
    an[Exception] shouldBe thrownBy(fs0.incrementsAfter(fs1))
    an[Exception] shouldBe thrownBy(fs1.incrementsAfter(fs2))
  }

}
