package io.liquirium.core

/**
  * Like an OperationRequest just without market
  */
sealed trait OperationIntent


object OperationIntent {

  case class CancelIntent(orderId: String) extends OperationIntent {

    def toOperationRequest(market: Market): OperationRequest = CancelRequest(market, orderId)

  }

  case class OrderIntent(quantity: BigDecimal, price: BigDecimal) extends OperationIntent {

    def toOperationRequest(market: Market, modifiers: Set[OrderModifier]): OperationRequest =
      OrderRequest(market, quantity = quantity, price = price, modifiers = modifiers)

    def volume: BigDecimal = (quantity * price).abs

  }

}
