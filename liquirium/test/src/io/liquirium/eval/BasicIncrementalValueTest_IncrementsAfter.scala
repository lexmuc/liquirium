package io.liquirium.eval

import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}

class BasicIncrementalValueTest_IncrementsAfter extends BasicIncrementalValueTest {

  test("it yields an empty sequence when the values are identical (eq)") {
    val r = Root(1)
    val v1 = r.inc(2)
    r.incrementsAfter(r) shouldEqual Seq()
    v1.incrementsAfter(v1) shouldEqual Seq()
  }

  test("it yields all the increments when the value is a descendant of the other (passed) value") {
    val v1 = Root(1).inc(2)
    v1.inc(3).inc(4).incrementsAfter(v1) shouldEqual Seq(3, 4)
  }

  test("it also works when the root is the single common ancestor") {
    val v1 = Root(1)
    v1.inc(2).inc(3).incrementsAfter(v1) shouldEqual Seq(2, 3)
  }

  test("if the value is no descendant of the passed value it throws an exception") {
    val v1 = Root(1).inc(2)
    an[Exception] shouldBe thrownBy(v1.incrementsAfter(v1.inc(3)))
    an[Exception] shouldBe thrownBy(v1.inc(2).incrementsAfter(v1.inc(3)))
    an[Exception] shouldBe thrownBy(Root(1).incrementsAfter(Root(2)))
  }

}
