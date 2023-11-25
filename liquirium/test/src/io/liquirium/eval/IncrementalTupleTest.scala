package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.helpers.IncrementalIntListWithRootValue

class IncrementalTupleTest extends BasicTest {

  private def intsWithRoot(root: Int)(ii: Int*) = IncrementalIntListWithRootValue(root)(ii: _*)

  private def strings(ss: String*) = ss.foldLeft(IncrementalSeq.empty[String])(_.inc(_))

  test("a tuple can be created from two incremental values and the values can be accessed individually") {
    val ii = intsWithRoot(0)(1, 2)
    val ss = strings("A", "B", "C")
    val tuple = IncrementalTuple(ii, ss)
    tuple._1 should be theSameInstanceAs ii
    tuple._2 should be theSameInstanceAs ss
  }

  test("the latest common ancestor is the combination of the elements latest common ancestors") {
    val ii = intsWithRoot(0)(1, 2)
    val ss = strings("A", "B", "C")
    val lca = IncrementalTuple(ii, ss)
    val t1 = IncrementalTuple(ii.inc(3).inc(4), ss.inc("D"))
    val t2 = IncrementalTuple(ii.inc(5).inc(6), ss)
    t1.latestCommonAncestor(t2) shouldEqual Some(lca)
    t2.latestCommonAncestor(t1) shouldEqual Some(lca)
  }

  test("there is no latest common ancestor if one of the elements is unrelated to the respective other") {
    val ii1 = intsWithRoot(1)(1, 2)
    val ii2 = intsWithRoot(2)(1, 2)
    val ss = strings("A", "B", "C")

    val t1 = IncrementalTuple(ii1, ss)
    val t2 = IncrementalTuple(ii2, ss)
    t1.latestCommonAncestor(t2) shouldEqual None
    t2.latestCommonAncestor(t1) shouldEqual None
    val t3 = IncrementalTuple(ss, ii1)
    val t4 = IncrementalTuple(ss, ii2)
    t3.latestCommonAncestor(t4) shouldEqual None
    t4.latestCommonAncestor(t3) shouldEqual None
  }

  test("the root is the combination of both elements roots") {
    val ii1 = intsWithRoot(1)(1, 2)
    val ii2 = intsWithRoot(2)(1, 2)
    IncrementalTuple(ii1, ii2).root shouldEqual IncrementalTuple(ii1.root, ii2.root)
  }

  test("one is only the ancestor of another if both its elements are ancestors of the other elements") {
    val ii = intsWithRoot(1)(1, 2)
    val ss = strings("A", "B", "C")
    val t1 = IncrementalTuple(ii, ss)
    val t2 = IncrementalTuple(ii.inc(3), ss.inc("D"))
    t1.isAncestorOf(t2) shouldBe true
    t2.isAncestorOf(t1) shouldBe false
    val t3 = IncrementalTuple(ii.inc(3), ss)
    t1.isAncestorOf(t3) shouldBe true
    t3.isAncestorOf(t1) shouldBe false
    val t4 = IncrementalTuple(ii, ss.inc("D"))
    t1.isAncestorOf(t4) shouldBe true
    t4.isAncestorOf(t1) shouldBe false
  }

  test("the increments after itself are empty") {
    val ii = intsWithRoot(1)(1, 2)
    val ss = strings("A", "B", "C")
    val t = IncrementalTuple(ii, ss)
    t.incrementsAfter(t) shouldEqual List()
  }

  test("the increments after another tuple are the increments after the first element first, then the second") {
    val ii = intsWithRoot(1)(1, 2)
    val ss = strings("A", "B")
    val t1 = IncrementalTuple(ii, ss)
    val t2 = IncrementalTuple(ii.inc(3).inc(4), ss.inc("C").inc("D"))
    t2.incrementsAfter(t1) shouldEqual Seq(Left(3), Left(4), Right("C"), Right("D"))
  }

  test("an exception is thrown when trying to obtain increments after a tuple that is not an ancestor") {
    val ii = intsWithRoot(1)(1, 2)
    val ss = strings("A", "B")
    val t1 = IncrementalTuple(ii, ss)
    val t2 = IncrementalTuple(ii.inc(3), ss)
    val t3 = IncrementalTuple(ii, ss.inc("C"))
    an[Exception] shouldBe thrownBy(t1.incrementsAfter(t3))
    an[Exception] shouldBe thrownBy(t1.incrementsAfter(t2))
  }

}
