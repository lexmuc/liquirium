package io.liquirium.util

import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CoreHelpers.dec

class NumberPrecisionTest extends BasicTest {

  def roundToDigitsAfterSeparator(n: Int)(p: BigDecimal): BigDecimal = NumberPrecision.digitsAfterSeparator(n).apply(p)

  def roundToMultiple(step: BigDecimal)(p: BigDecimal): BigDecimal = NumberPrecision.multipleOf(step).apply(p)

  def roundToSignificantDigits(n: Int, decimalsAfterPoint: Option[Int] = None)(p: BigDecimal): BigDecimal =
    NumberPrecision.significantDigits(n, maxDecimalsAfterPoint = decimalsAfterPoint).apply(p)

  // digits after separator

  test("the price can be rounded to a certain number of digits after the point") {
    roundToDigitsAfterSeparator(2)(dec("12.344")) shouldEqual dec("12.34")
    roundToDigitsAfterSeparator(2)(dec("12.345")) shouldEqual dec("12.35")
    roundToDigitsAfterSeparator(2)(dec("12.346")) shouldEqual dec("12.35")
  }

  test("next higher and lower prices can be obtained for decimals-after-point precision") {
    val p = NumberPrecision.digitsAfterSeparator(2)
    p.nextHigher(dec("1.236")) shouldEqual dec("1.24")
    p.nextHigher(dec("1.234")) shouldEqual dec("1.24")
    p.nextHigher(dec("1.23")) shouldEqual dec("1.24")

    p.nextLower(dec("1.236")) shouldEqual dec("1.23")
    p.nextLower(dec("1.24")) shouldEqual dec("1.23")
    p.nextLower(dec("1.234")) shouldEqual dec("1.23")
  }

  // multiple of

  test("the multiple-of precision rounds to the respectively nearest matching price") {
    roundToMultiple(dec("0.5"))(dec("3.1")) shouldEqual dec("3.0")
    roundToMultiple(dec("0.5"))(dec("3.4")) shouldEqual dec("3.5")
    roundToMultiple(dec("0.5"))(dec("3.25")) shouldEqual dec("3.5")
  }

  test("next higher and lower prices can be obtained for multiple-of precision") {
    val p = NumberPrecision.multipleOf(0.5)
    p.nextHigher(dec("1.2")) shouldEqual dec("1.5")
    p.nextHigher(dec("1.3")) shouldEqual dec("1.5")
    p.nextHigher(dec("1.5")) shouldEqual dec("2.0")

    p.nextLower(dec("1.2")) shouldEqual dec("1.0")
    p.nextLower(dec("1.3")) shouldEqual dec("1.0")
    p.nextLower(dec("1.5")) shouldEqual dec("1.0")
  }

  test("multipleOf can be converted to a meaningful string") {
    val p = NumberPrecision.digitsAfterSeparator(2)
    p.toString shouldEqual "multipleOf(0.01)"
  }


  // inverse multiple of

  test("the inverse multiple-of precision changes as if the inverse price was rounded to the next matching price") {
    def p(step: BigDecimal) = NumberPrecision.inverseMultipleOf(step)
    p(dec("0.25"))(dec("2.1")) shouldEqual dec("2")
    p(dec("0.25"))(dec("1.9")) shouldEqual dec("2")
    p(dec("4"))(dec("0.2")) shouldEqual dec("0.25")
  }

  test("next higher and lower prices can be obtained for inverse multiple-of precision") {
    val p = NumberPrecision.inverseMultipleOf(0.5)
    // prices are 2, 1, 0.666..., 0.5, 0.4
    p.nextHigher(dec("0.7")) shouldEqual dec("1.0")
    p.nextHigher(dec("1.0")) shouldEqual dec("2.0")

    p.nextLower(dec("1.2")) shouldEqual dec("1.0")
    p.nextLower(dec("0.5")) shouldEqual dec("0.4")
  }

  test("inverseMultipleOf can be converted to a meaningful string") {
    val p = NumberPrecision.inverseMultipleOf(0.5)
    p.toString shouldEqual "inverseMultipleOf(0.5)"
  }

  // significant digits

  test("the price can be rounded to a certain number of significant digits") {
    roundToSignificantDigits(5)(dec("12.34444")) shouldEqual dec("12.344")
    roundToSignificantDigits(5)(dec("12.3451")) shouldEqual dec("12.345")
    roundToSignificantDigits(5)(dec("12.3461")) shouldEqual dec("12.346")

    roundToSignificantDigits(3)(dec("1234")) shouldEqual dec("1230")
    roundToSignificantDigits(3)(dec("1235")) shouldEqual dec("1240")

    roundToSignificantDigits(3)(dec("0.01234")) shouldEqual dec("0.0123")
    roundToSignificantDigits(3)(dec("0.01235")) shouldEqual dec("0.0124")
  }

  test("next higher and lower prices can be obtained for significant digits precision") {
    val twoDigitPrecision = NumberPrecision.significantDigits(2)
    twoDigitPrecision.nextHigher(dec("123")) shouldEqual dec("130")
    twoDigitPrecision.nextHigher(dec("129")) shouldEqual dec("130")
    twoDigitPrecision.nextHigher(dec("130")) shouldEqual dec("140")
    twoDigitPrecision.nextLower(dec("123")) shouldEqual dec("120")
    twoDigitPrecision.nextLower(dec("129")) shouldEqual dec("120")
    twoDigitPrecision.nextLower(dec("130")) shouldEqual dec("120")

    twoDigitPrecision.nextHigher(dec("0.0123")) shouldEqual dec("0.0130")
    twoDigitPrecision.nextHigher(dec("0.0129")) shouldEqual dec("0.0130")
    twoDigitPrecision.nextHigher(dec("0.0130")) shouldEqual dec("0.0140")
    twoDigitPrecision.nextLower(dec("0.0123")) shouldEqual dec("0.0120")
    twoDigitPrecision.nextLower(dec("0.0129")) shouldEqual dec("0.0120")
    twoDigitPrecision.nextLower(dec("0.0130")) shouldEqual dec("0.0120")
  }

  test("rounding to significant digits and moving to higher and lower prices works for edge cases") {
    val twoDigitPrecision = NumberPrecision.significantDigits(2)
    roundToSignificantDigits(2)(dec("999")) shouldEqual dec("1000")
    twoDigitPrecision.nextHigher(dec("999")) shouldEqual dec("1000")
    twoDigitPrecision.nextHigher(dec("990")) shouldEqual dec("1000")
    roundToSignificantDigits(2)(dec("101")) shouldEqual dec("100")
    twoDigitPrecision.nextLower(dec("1000")) shouldEqual dec("990")
    twoDigitPrecision.nextLower(dec("1001")) shouldEqual dec("1000")
  }

  test("significant digits can be converted to a meaningful string") {
    val p = NumberPrecision.significantDigits(2)
    p.toString shouldEqual "significantDigits(2)"
  }

  // significant digits with extra number of decimal places after point

  test("extra digits are removed (with rounding) only when they exceed the maximum decimals after the point") {
    roundToSignificantDigits(5, Some(2))(dec("12.34444")) shouldEqual dec("12.34")
    roundToSignificantDigits(5, Some(2))(dec("12.3451")) shouldEqual dec("12.35")
    roundToSignificantDigits(5, Some(3))(dec("12.3461")) shouldEqual dec("12.346")
  }

  test("removing extra digits after moving to next higher number can in some cases lead to an even higher number") {
    def precision(sig: Int, maxDec: Int) = NumberPrecision.significantDigits(sig, Some(maxDec))
    precision(2, 2).nextHigher(dec("0.0123")) shouldEqual dec("0.02")
    precision(2, 3).nextHigher(dec("0.0123")) shouldEqual dec("0.013")
  }

  test("removing extra digits after moving to next lower number can in some cases lead to an even lower number") {
    def precision(sig: Int, maxDec: Int) = NumberPrecision.significantDigits(sig, Some(maxDec))
    precision(2, 2).nextLower(dec("0.0123")) shouldEqual dec("0.01")
    precision(2, 3).nextLower(dec("0.0123")) shouldEqual dec("0.012")
  }

  test("significant digits with decimals limit can be converted to a meaningful string") {
    val p = NumberPrecision.significantDigits(2, Some(1))
    p.toString shouldEqual "significantDigits(2, decimals=1)"
  }

}
