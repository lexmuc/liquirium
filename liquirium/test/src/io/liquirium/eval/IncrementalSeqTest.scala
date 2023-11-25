package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest

class IncrementalSeqTest extends BasicTest {

  private def incSeq(ints: Int*) = ints.foldLeft(IncrementalSeq.empty[Int])(_.inc(_))

  test("the empty sequence has length zero and equals an empty list") {
    IncrementalSeq.empty[Int].length shouldEqual 0
    IncrementalSeq.empty[Int] shouldEqual List[Int]()
  }

  test("the empty sequence has no previous element and no last increment") {
    IncrementalSeq.empty[Int].prev shouldBe None
    IncrementalSeq.empty[Int].lastIncrement shouldBe None
  }

  test("accessing any index in an empty sequence yields an IndexOutOfBoundsException") {
    an[IndexOutOfBoundsException] shouldBe thrownBy(IncrementalSeq.empty[Int](0))
  }

  test("incrementing the list appends elements and increases the length") {
    val l0 = IncrementalSeq.empty[Int]
    val l1 = l0.inc(1)
    val l2 = l1.inc(2)
    l1.length shouldBe 1
    l1 shouldEqual List(1)
    l2.length shouldBe 2
    l2 shouldEqual List(1, 2)
  }

  test("non-empty lists have previous element and last increment properly set") {
    val l0 = IncrementalSeq.empty[Int]
    val l1 = l0.inc(1)
    val l2 = l1.inc(2)
    l1.prev shouldBe Some(l0)
    l1.lastIncrement shouldBe Some(1)
    l2.prev shouldBe Some(l1)
    l2.lastIncrement shouldBe Some(2)
  }

  test("in non-empty list elements can be accessed by index and invalid indexes yield an exception") {
    val l0 = IncrementalSeq.empty[Int]
    val l1 = l0.inc(1)
    val l2 = l1.inc(2)
    l2(0) shouldEqual 1
    l2(1) shouldEqual 2
    an[IndexOutOfBoundsException] shouldBe thrownBy(l2(2))
  }

  test("it can be created from an iterable") {
    val i = List(1, 2, 3).asInstanceOf[Iterable[Int]]
    IncrementalSeq.from(i) shouldEqual IncrementalSeq.empty.inc(1).inc(2).inc(3)
  }

  test("it can be created from a series of elements") {
    IncrementalSeq(1, 2, 3) shouldEqual IncrementalSeq.empty.inc(1).inc(2).inc(3)
  }

  test("equality checks with other incremental sequences work properly") {
    incSeq() shouldEqual incSeq()
    incSeq(1, 2, 3) shouldEqual incSeq(1, 2, 3)
  }

  test("equality checks with other sequences work properly") {
    incSeq() shouldEqual Seq()
    incSeq(1, 2, 3) shouldEqual Seq(1, 2, 3)
  }

  test("the reverse iterator works properly (may be overridden for performance reasons)") {
    incSeq(1, 2, 3, 4).reverseIterator.take(3).toList shouldEqual List(4, 3, 2)
  }

  test("equality checks with different values all yield false") {
    incSeq(1, 2, 3) should not equal Set(1, 2, 3)
    incSeq() should not equal 1
  }

  test("the last element can be accessed via lastOption (test our more efficient implementation)") {
    incSeq(1, 2, 3).lastOption shouldEqual Some(3)
    incSeq().lastOption shouldEqual None
  }

}
