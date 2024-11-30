package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class ExactResourcesTest extends BasicTest {

  def res(base: String, quote: String): ExactResources =
    ExactResources(baseBalance = dec(base), quoteBalance = dec(quote))

  test("the empty resources have quote and base balance 0") {
    ExactResources.empty.quoteBalance shouldBe dec(0)
    ExactResources.empty.baseBalance shouldBe dec(0)
  }

  test("the value includes the quote balance and the value of the base balance at the given price") {
    res(base = "2.5", quote = "1.5").valueAt(dec(2)) shouldBe dec("6.5")
  }

  test("resources are strictly greater than others if both money and position are greater") {
    res(base = "2.6", quote = "1.5").areStrictlyGreaterThan(res(base = "2.5", quote = "1.5")) shouldBe false
    res(base = "2.5", quote = "1.6").areStrictlyGreaterThan(res(base = "2.5", quote = "1.5")) shouldBe false
    res(base = "2.6", quote = "1.6").areStrictlyGreaterThan(res(base = "2.5", quote = "1.5")) shouldBe true
  }

  test("resources can be added") {
    res(base = "2.6", quote = "1.5")
      .plus(res(base = "1.2", quote = "1.3")) shouldEqual
      res(base = "3.8", quote = "2.8")
  }

  test("it is possible to add or subtract only base or quoteBalance") {
    res(base = "2.6", quote = "1.5").plusQuote(dec("1")) shouldEqual res(base = "2.6", quote = "2.5")
    res(base = "2.6", quote = "1.5").plusBase(dec("1")) shouldEqual res(base = "3.6", quote = "1.5")
    res(base = "2.6", quote = "1.5").minusQuote(dec("1")) shouldEqual res(base = "2.6", quote = "0.5")
    res(base = "2.6", quote = "1.5").minusBase(dec("1")) shouldEqual res(base = "1.6", quote = "1.5")
  }

  test("the exposure can be obtained as a Double for a given price") {
    res(base = "2.5", quote = "1.5").exposureAt(dec(1)) shouldEqual 0.625d
    res(base = "3", quote = "2").exposureAt(dec(2)) shouldEqual 0.75d
    res(base = "6", quote = "-6").exposureAt(dec(2)) shouldEqual 2d
    res(base = "-3", quote = "12").exposureAt(dec(2)) shouldEqual -1d
  }

  test("they can be converted to double resources") {
    res(base = "2.5", quote = "1.5").toDoubleResources shouldEqual Resources(1.5, baseBalance = 2.5)
  }

  test("it can record buys and sells") {
    res(base = "2", quote = "1").recordTradeEffect(dec(10), price = dec("0.5")) shouldEqual
      res(base = "12", quote = "-4")
    res(base = "2", quote = "1").recordTradeEffect(dec(-10), price = dec("0.5")) shouldEqual
      res(base = "-8", quote = "6")
  }

  test("flipping resources just exchanges money and position") {
    res(base = "2", quote = "1").flip  shouldEqual res(base = "1", quote = "2")
  }

}
