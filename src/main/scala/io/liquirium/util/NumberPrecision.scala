package io.liquirium.util

import java.math.{MathContext, RoundingMode => JavaRoundingMode}
import scala.math.BigDecimal.RoundingMode

trait NumberPrecision extends Function[BigDecimal, BigDecimal] {

  def nextHigher(p: BigDecimal): BigDecimal
  def nextLower(p: BigDecimal): BigDecimal

}

object NumberPrecision {

  def digitsAfterSeparator(n: Int): NumberPrecision = multipleOf(BigDecimal(1) / BigDecimal(10).pow(n))

  def multipleOf(step: BigDecimal): NumberPrecision = MultipleOf(step)

  def inverseMultipleOf(step: BigDecimal): NumberPrecision = InverseMultipleOf(step)

  def significantDigits(n: Int, maxDecimalsAfterPoint: Option[Int] = None): NumberPrecision =
    SignificantDigits(n, maxDecimalsAfterPoint)

  case class SignificantDigits(
    numberOfDigits: Int,
    maxDecimalsAfterPoint: Option[Int],
  ) extends NumberPrecision {
    private val maxDecimalsAfterPointPrecision = maxDecimalsAfterPoint.map(digitsAfterSeparator)

    private val mc = new MathContext(numberOfDigits, JavaRoundingMode.HALF_UP)
    private val mcUp = new MathContext(numberOfDigits, JavaRoundingMode.UP)
    private val mcDown = new MathContext(numberOfDigits, JavaRoundingMode.DOWN)

    private val relativeDelta: BigDecimal = BigDecimal(1) / BigDecimal(10).pow(numberOfDigits)
    private val relativeDeltaUp: BigDecimal = BigDecimal(1) + relativeDelta
    private val relativeDeltaDown: BigDecimal = BigDecimal(1) - relativeDelta

    override def apply(p: BigDecimal): BigDecimal = maxDecimalsAfterPointPrecision match {
      case Some(extraPrecision) => extraPrecision.apply(round(p))
      case None => round(p)
    }

    private def round(p: BigDecimal): BigDecimal = p.round(mc)

    override def nextHigher(p: BigDecimal): BigDecimal = {
      val sigOnly = {
        val rounded = p.round(mcUp)
        if (rounded > p) rounded
        else (p * relativeDeltaUp).round(mcUp)
      }
      maxDecimalsAfterPointPrecision match {
        case Some(extraPrecision) =>
          val extraOnly = extraPrecision.nextHigher(p)
          if (extraOnly > sigOnly) extraOnly else sigOnly
        case None => sigOnly
      }
    }

    override def nextLower(p: BigDecimal): BigDecimal = {
      val sigOnly = {
        val rounded = p.round(mcDown)
        if (rounded < p) rounded
        else (p * relativeDeltaDown).round(mcDown)
      }
      maxDecimalsAfterPointPrecision match {
        case Some(extraPrecision) =>
          val extraOnly = extraPrecision.nextLower(p)
          if (extraOnly < sigOnly) extraOnly else sigOnly
        case None => sigOnly
      }
    }

    override def toString(): String = maxDecimalsAfterPoint match {
      case Some(d) => "significantDigits(" + numberOfDigits + ", decimals=" + d + ")"
      case None => "significantDigits(" + numberOfDigits + ")"
    }

  }

  case class MultipleOf(step: BigDecimal) extends NumberPrecision {

    override def apply(p: BigDecimal): BigDecimal = (p / step).setScale(0, RoundingMode.HALF_UP) * step

    override def nextHigher(p: BigDecimal): BigDecimal = {
      val applied = apply(p)
      if (p < applied) applied
      else applied + step
    }

    override def nextLower(p: BigDecimal): BigDecimal = {
      val applied = apply(p)
      if (p > applied) applied
      else applied - step
    }

    override def toString(): String = "multipleOf(" + step + ")"

  }

  case class InverseMultipleOf(step: BigDecimal) extends NumberPrecision {

    private val ONE: BigDecimal = BigDecimal(1)
    private val basePrecision: NumberPrecision = multipleOf(step)

    override def apply(p: BigDecimal): BigDecimal = ONE / basePrecision(ONE / p)

    override def nextHigher(p: BigDecimal): BigDecimal = ONE / basePrecision.nextLower(ONE / p)

    override def nextLower(p: BigDecimal): BigDecimal = ONE / basePrecision.nextHigher(ONE / p)

    override def toString(): String = "inverseMultipleOf(" + step + ")"

  }

}
