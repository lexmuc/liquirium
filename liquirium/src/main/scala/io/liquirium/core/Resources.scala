package io.liquirium.core

object Resources {
  def empty: Resources = Resources(quoteBalance = 0, baseBalance = 0)
}

case class Resources(quoteBalance: Double, baseBalance: Double) {

  def valueAt(price: Double): Double = quoteBalance + baseBalance * price

  def areStrictlyGreaterThan(other: Resources): Boolean =
    quoteBalance > other.quoteBalance && baseBalance > other.baseBalance

  def plus(other: Resources): Resources = Resources(
    quoteBalance = quoteBalance + other.quoteBalance,
    baseBalance = baseBalance + other.baseBalance
  )

  def plusQuote(m: Double): Resources = copy(quoteBalance = quoteBalance + m)

  def minusQuote(m: Double): Resources = copy(quoteBalance = quoteBalance - m)

  def plusBase(p: Double): Resources = copy(baseBalance = baseBalance + p)

  def minusBase(p: Double): Resources = copy(baseBalance = baseBalance - p)

  def exposureAt(price: Double): Double = baseBalance * price / valueAt(price)

  def scale(factor: Double): Resources = copy(
    quoteBalance = quoteBalance * factor,
    baseBalance = baseBalance * factor,
  )

  def flip: Resources = copy(
    quoteBalance = baseBalance,
    baseBalance = quoteBalance,
  )

}
