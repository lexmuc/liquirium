package io.liquirium.core

object ExactResources {
  def empty: ExactResources = ExactResources(quoteBalance = BigDecimal(0), baseBalance = BigDecimal(0))
}

case class ExactResources(
  quoteBalance: BigDecimal,
  baseBalance: BigDecimal,
) {

  def valueAt(price: BigDecimal): BigDecimal = quoteBalance + baseBalance * price

  def areStrictlyGreaterThan(other: ExactResources): Boolean =
    quoteBalance > other.quoteBalance && baseBalance > other.baseBalance

  def plus(other: ExactResources): ExactResources = ExactResources(
    quoteBalance = quoteBalance + other.quoteBalance,
    baseBalance = baseBalance + other.baseBalance
  )

  def plusQuote(m: BigDecimal): ExactResources = copy(quoteBalance = quoteBalance + m)

  def minusQuote(m: BigDecimal): ExactResources = copy(quoteBalance = quoteBalance - m)

  def plusBase(p: BigDecimal): ExactResources = copy(baseBalance = baseBalance + p)

  def minusBase(p: BigDecimal): ExactResources = copy(baseBalance = baseBalance - p)

  def exposureAt(price: BigDecimal): Double = {
    val positionDoubleValue = baseBalance.toDouble * price.toDouble
    positionDoubleValue / (quoteBalance.toDouble + positionDoubleValue)
  }

  def toDoubleResources: Resources = Resources(quoteBalance.toDouble, baseBalance = baseBalance.toDouble)

  def recordTradeEffect(quantityChange: BigDecimal, price: BigDecimal): ExactResources =
    plusBase(quantityChange).minusQuote(price * quantityChange)

  def flip: ExactResources = ExactResources(quoteBalance = baseBalance, baseBalance = quoteBalance)

}
