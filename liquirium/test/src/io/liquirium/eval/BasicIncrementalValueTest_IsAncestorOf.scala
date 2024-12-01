package io.liquirium.eval

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BasicIncrementalValueTest_IsAncestorOf extends BasicIncrementalValueTest {

  test("it works properly in cases where a root is involved") {
    val r = Root(1)
    r.isAncestorOf(r) shouldBe true
    r.isAncestorOf(Root(2)) shouldBe false
    r.inc(1).isAncestorOf(r) shouldBe false
    r.isAncestorOf(r.inc(1)) shouldBe true
    r.isAncestorOf(r.inc(1).inc(2)) shouldBe true
  }

  test("it works properly in cases where only non-root values are involved") {
    val r = Root(1)
    val v = r.inc(2)
    v.isAncestorOf(v) shouldBe true
    v.inc(1).isAncestorOf(v) shouldBe false
    v.isAncestorOf(v.inc(1)) shouldBe true
    v.isAncestorOf(v.inc(1).inc(2)) shouldBe true
  }

  test("the value is not considered an ancestor of the other value when both are based on different instances") {
    Root(1).isAncestorOf(Root(1)) shouldBe false
    Root(1).isAncestorOf(Root(1).inc(2)) shouldBe false
    Root(1).inc(1).isAncestorOf(Root(1).inc(1)) shouldBe false
    Root(1).inc(1).isAncestorOf(Root(1).inc(1).inc(2)) shouldBe false
    val r = Root(1)
    r.inc(1).isAncestorOf(r.inc(1).inc(2)) shouldBe false
  }

}
