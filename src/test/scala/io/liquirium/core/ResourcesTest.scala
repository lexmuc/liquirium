package io.liquirium.core

import io.liquirium.core.helpers.BasicTest

class ResourcesTest extends BasicTest {

  private def res(base: Double, quote: Double): Resources =
    Resources(baseBalance = base, quoteBalance = quote)

  test("the empty resources have money and position 0") {
    Resources.empty.quoteBalance shouldBe 0.0
    Resources.empty.baseBalance shouldBe 0.0
  }

  test("the value includes the money and the value of the position at the given price") {
    res(base = 2.5, quote = 1.5).valueAt(2) shouldBe 6.5
  }

  test("resources are strictly greater than others if both money and position are greater") {
    res(base = 2.6, quote = 1.5).areStrictlyGreaterThan(res(base = 2.5, quote = 1.5)) shouldBe false
    res(base = 2.5, quote = 1.6).areStrictlyGreaterThan(res(base = 2.5, quote = 1.5)) shouldBe false
    res(base = 2.6, quote = 1.6).areStrictlyGreaterThan(res(base = 2.5, quote = 1.5)) shouldBe true
  }

  test("resources can be added") {
    res(base = 2.5, quote = 1.5).plus(res(base = 2.0, quote = 1.0)) shouldEqual res(base = 4.5, quote = 2.5)
  }

  test("it is possible to add or subtract only money or position") {
    res(base = 2.5, quote = 1.5).plusQuote(1) shouldEqual res(base = 2.5, quote = 2.5)
    res(base = 2.5, quote = 1.5).minusQuote(1) shouldEqual res(base = 2.5, quote = 0.5)
    res(base = 2.5, quote = 1.5).plusBase(1) shouldEqual res(base = 3.5, quote = 1.5)
    res(base = 2.5, quote = 1.5).minusBase(1) shouldEqual res(base = 1.5, quote = 1.5)
  }

  test("the exposure can be obtained for a given price") {
    res(base = 2.5, quote = 1.5).exposureAt(1) shouldEqual 0.625
    res(base = 3, quote = 2).exposureAt(2) shouldEqual 0.75
    res(base = 6, quote = -6).exposureAt(2) shouldEqual 2
    res(base = -3, quote = 12).exposureAt(2) shouldEqual -1
  }

  test("they can be scaled with a given factor") {
    res(base = 2.5, quote = 1.5).scale(2.0) shouldEqual res(base = 5.0, quote = 3.0)
    res(base = 2.5, quote = 1.5).scale(-2.0) shouldEqual res(base = -5.0, quote = -3.0)
  }

  test("flipping resources just exchanges base and quote") {
    res(base = 3.0, quote = 2.0).flip shouldEqual res(base = 2.0, quote = 3.0)
  }

}
