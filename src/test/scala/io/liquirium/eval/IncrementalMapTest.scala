package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest

class IncrementalMapTest extends BasicTest {

  val emptyMap: IncrementalMap[Int, String] = IncrementalMap.empty[Int, String]

  test("the empty incremental map has the empty Map as value") {
    emptyMap.mapValue shouldEqual Map()
  }

  test("the map value reflects updates to the map") {
    emptyMap.update(1, "A").update(2, "B").update(1, "C").mapValue shouldEqual Map(1 -> "C", 2 -> "B")
  }

  test("keys can be deleted from the map as well") {
    emptyMap.update(1, "A").update(2, "B").deleteKey(1).mapValue shouldEqual Map(2 -> "B")
  }

  test("relative updates can be obtained as tuples for a map derived from another one") {
    val m0 = emptyMap.update(1, "A")
    val m1 = m0.update(2, "B").update(1, "C").deleteKey(1)
    m1.incrementsAfter(m0) shouldEqual List(2 -> Some("B"), 1 -> Some("C"), 1 -> None)
  }

  test("an exception is thrown when trying to get updates relative to a map that is not a descendant") {
    val m1 = emptyMap.update(1, "A")
    val m2 = emptyMap.update(1, "B")
    an[Exception] shouldBe thrownBy(m1.incrementsAfter(m2))
    an[Exception] shouldBe thrownBy(m2.incrementsAfter(m1))
  }

  test("apply works directly on the incremental map") {
    val m = emptyMap.update(1, "A").update(2, "B")
    m(1) shouldEqual "A"
    m(2) shouldEqual "B"
  }

  test("the root value is an empty map") {
    val e = emptyMap
    val m = e.update(1, "A").update(2, "B")
    e.root shouldEqual e
    m.root shouldEqual e
  }

  test("it can be determined if one map is the ancestor of another one") {
    val e = emptyMap
    val m = e.update(1, "A").update(2, "B").deleteKey(1)
    e.isAncestorOf(e) shouldBe true
    m.isAncestorOf(e) shouldBe false
    e.isAncestorOf(m) shouldBe true
    m.update(1, "X").isAncestorOf(m) shouldBe false
    m.isAncestorOf(m.update(1, "X")) shouldBe true
  }

  test("the latest common ancestor can be determined") {
    val e = emptyMap
    val m = e.update(1, "A").update(2, "B").deleteKey(1)
    e.latestCommonAncestor(e).get shouldEqual e
    e.latestCommonAncestor(m).get shouldEqual e
    m.latestCommonAncestor(e).get shouldEqual e
    m.update(1, "C").latestCommonAncestor(m).get shouldEqual m
    m.latestCommonAncestor(m.update(1, "C")).get shouldEqual m
  }

  test("getting updates after latest common ancestor work properly") {
    val e = emptyMap
    val m = e.update(1, "A").update(2, "B").deleteKey(1)
    e.incrementsAfter(e) shouldEqual List()
    m.incrementsAfter(e) shouldEqual List((1, Some("A")), (2, Some("B")), (1, None))
    m.update(1, "C").incrementsAfter(m) shouldEqual List((1, Some("C")))
  }

  test("getting updates after a map not being an ancestor yields an exception") {
    val e = emptyMap
    val m = e.update(1, "A").update(2, "B")
    an[Exception] shouldBe thrownBy(e.incrementsAfter(m))
    an[Exception] shouldBe thrownBy(m.incrementsAfter(m.update(1, "C")))
  }

}
