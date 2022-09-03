package io.liquirium.core

import scala.math.BigDecimal.RoundingMode

trait OrderQuantityPrecision extends Function[BigDecimal, BigDecimal]

object OrderQuantityPrecision {

  case object Infinite extends OrderQuantityPrecision {

    override def apply(p: BigDecimal): BigDecimal = p

  }

  case class DigitsAfterSeparator(n: Int) extends OrderQuantityPrecision {

    override def apply(p: BigDecimal): BigDecimal = p.setScale(n, RoundingMode.DOWN)

  }

  case class MultipleOf(step: BigDecimal) extends OrderQuantityPrecision {

    override def apply(p: BigDecimal): BigDecimal = (p / step).setScale(0, RoundingMode.DOWN) * step

  }

}
