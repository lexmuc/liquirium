package io.liquirium.core

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec

class OrderQuantityPrecisionTest extends BasicTest {

  def roundToDigitsAfterSeparator(n: Int)(p: BigDecimal): BigDecimal =
    OrderQuantityPrecision.DigitsAfterSeparator(n).apply(p)

  def roundToMultipleOf(step: BigDecimal)(p: BigDecimal): BigDecimal =
    OrderQuantityPrecision.MultipleOf(step).apply(p)

  test("the infinite precision does not change the quantity") {
    OrderQuantityPrecision.Infinite(dec("0.123456789")) shouldEqual dec("0.123456789")
  }

  test("the quantity can be rounded to a certain number of digits after the separator but it is always rounded down") {
    roundToDigitsAfterSeparator(2)(dec("12.344")) shouldEqual dec("12.34")
    roundToDigitsAfterSeparator(2)(dec("12.346")) shouldEqual dec("12.34")
    roundToDigitsAfterSeparator(2)(dec("-12.344")) shouldEqual dec("-12.34")
    roundToDigitsAfterSeparator(2)(dec("-12.346")) shouldEqual dec("-12.34")
  }

  test("the quantity can be rounded to a multiple of a given value but it is always rounded down") {
    roundToMultipleOf(dec("0.5"))(dec("3.1")) shouldEqual dec("3.0")
    roundToMultipleOf(dec("0.5"))(dec("3.4")) shouldEqual dec("3.0")
    roundToMultipleOf(dec("0.5"))(dec("3.5")) shouldEqual dec("3.5")
    roundToMultipleOf(dec("0.5"))(dec("-3.1")) shouldEqual dec("-3.0")
    roundToMultipleOf(dec("0.5"))(dec("-3.4")) shouldEqual dec("-3.0")
    roundToMultipleOf(dec("0.5"))(dec("-3.5")) shouldEqual dec("-3.5")
  }

}
