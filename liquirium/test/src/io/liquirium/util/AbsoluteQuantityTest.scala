package io.liquirium.util

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec

class AbsoluteQuantityTest extends BasicTest {

  test("an exception is thrown only when trying to create an absolute quantity with negative value") {
    AbsoluteQuantity(dec(1))
    AbsoluteQuantity(dec(0))
    a[RuntimeException] shouldBe thrownBy(AbsoluteQuantity(dec(-1)))
  }

}
