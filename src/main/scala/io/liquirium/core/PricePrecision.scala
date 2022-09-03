package io.liquirium.core

import java.math.{MathContext, RoundingMode => JavaRoundingMode}
import scala.math.BigDecimal.RoundingMode

trait PricePrecision extends Function[BigDecimal, BigDecimal] {

  def nextHigher(p: BigDecimal): BigDecimal
  def nextLower(p: BigDecimal): BigDecimal

}

object PricePrecision {

  def digitsAfterSeparator(n: Int): PricePrecision = multipleOf(BigDecimal(1) / BigDecimal(10).pow(n))

  def multipleOf(step: BigDecimal): PricePrecision = MultipleOf(step)

  def inverseMultipleOf(step: BigDecimal): PricePrecision = InverseMultipleOf(step)

  def significantDigits(n: Int, maxDecimalsAfterPoint: Option[Int] = None): PricePrecision =
    SignificantDigits(n, maxDecimalsAfterPoint.map(digitsAfterSeparator))

  case object Infinite extends PricePrecision {

    override def apply(p: BigDecimal): BigDecimal = p

    override def nextHigher(p: BigDecimal): BigDecimal = p

    override def nextLower(p: BigDecimal): BigDecimal = p

  }

  case class SignificantDigits(
    numberOfDigits: Int,
    maxDecimalsAfterPointPrecision: Option[PricePrecision],
  ) extends PricePrecision {

    val mc = new MathContext(numberOfDigits, JavaRoundingMode.HALF_UP)
    val mcUp = new MathContext(numberOfDigits, JavaRoundingMode.UP)
    val mcDown = new MathContext(numberOfDigits, JavaRoundingMode.DOWN)

    val relativeDelta: BigDecimal = BigDecimal(1) / BigDecimal(10).pow(numberOfDigits)
    val relativeDeltaUp: BigDecimal = BigDecimal(1) + relativeDelta
    val relativeDeltaDown: BigDecimal = BigDecimal(1) - relativeDelta

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

  }

  case class MultipleOf(step: BigDecimal) extends PricePrecision {

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

  }

  case class InverseMultipleOf(step: BigDecimal) extends PricePrecision {

    val ONE: BigDecimal = BigDecimal(1)
    val basePrecision: PricePrecision = multipleOf(step)

    override def apply(p: BigDecimal): BigDecimal = ONE / basePrecision(ONE / p)

    override def nextHigher(p: BigDecimal): BigDecimal = ONE / basePrecision.nextLower(ONE / p)

    override def nextLower(p: BigDecimal): BigDecimal = ONE / basePrecision.nextHigher(ONE / p)

  }

}
