package io.liquirium.eval

class BasicIncrementalValueTest_Root extends BasicIncrementalValueTest {

  test("the root of the root is the root") {
    val r = Root(1)
    r.root should be theSameInstanceAs r
  }

  test("the root of any derived value can be determined") {
    val r = Root(1)
    r.inc(1).root should be theSameInstanceAs r
    r.inc(1).inc(2).root should be theSameInstanceAs r
  }

}
