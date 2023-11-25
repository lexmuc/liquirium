package io.liquirium.connect.deribit

case object DeribitSettlement {

  sealed trait Type

  case object Delivery extends Type

  case object Settlement extends Type

}

case class DeribitSettlement
(
  indexPrice: BigDecimal,
  instrumentName: String,
  markPrice: BigDecimal,
  position: BigDecimal,
  profitLoss: BigDecimal,
  sessionProfitLoss: BigDecimal,
  timestamp: Long,
  `type`: DeribitSettlement.Type
) {

  def isFutureSettlement: Boolean = instrumentName.count(_ == '-') == 1

}
