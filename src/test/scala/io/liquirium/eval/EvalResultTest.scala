package io.liquirium.eval

import io.liquirium.core.helpers.BasicTest

class EvalResultTest extends BasicTest {

  test("an exception is thrown when an input request is empty") {
    a[RuntimeException] shouldBe thrownBy(InputRequest(Set()))
  }

}
