package io.liquirium.eval

class BasicIncrementalValueTest_LatestCommonAncestor extends BasicIncrementalValueTest {

  test("test no latest common ancestor is found if there is none") {
    val v1 = Root(1).inc(2)
    val v2 = Root(2).inc(2)
    v1.latestCommonAncestor(v2) shouldEqual None
  }

  test("if there are common ancestors, the latest one is found") {
    val v1 = Root(1).inc(2)
    val v2 = v1.inc(3)
    val v3 = v1.inc(4).inc(5)
    v2.latestCommonAncestor(v3).get should be theSameInstanceAs v1
    v3.latestCommonAncestor(v2).get should be theSameInstanceAs v1
  }

  test("the latest common ancestor may be the root") {
    val v1 = Root(1)
    val v2 = v1.inc(3)
    val v3 = v1.inc(4).inc(5)
    v2.latestCommonAncestor(v3).get should be theSameInstanceAs v1
    v3.latestCommonAncestor(v2).get should be theSameInstanceAs v1
  }

  test("the latest common ancestor may be one of the two values") {
    val v1 = Root(1)
    val v2 = v1.inc(4).inc(5)
    v1.latestCommonAncestor(v2).get should be theSameInstanceAs v1
    v2.latestCommonAncestor(v1).get should be theSameInstanceAs v1
  }

}
