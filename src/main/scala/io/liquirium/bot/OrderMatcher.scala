package io.liquirium.bot

import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.Order

trait OrderMatcher {

  def apply(order: Order.BasicOrderData, intent: OrderIntent): Boolean

}

object OrderMatcher {

  object ExactMatcher extends OrderMatcher {
    override def apply(order: Order.BasicOrderData, intent: OrderIntent): Boolean =
      order.price == intent.price && order.openQuantity == intent.quantity
  }

  case class TolerantMatcher(priceTolerance: Double, quantityTolerance: Double) extends OrderMatcher {
    override def apply(order: Order.BasicOrderData, intent: OrderIntent): Boolean =
      matchPrices(order.price, intent.price) && matchQuantity(order.openQuantity, intent.quantity)

    private def matchPrices(orderPrice: BigDecimal, intentPrice: BigDecimal): Boolean =
      orderPrice >= intentPrice / priceTolerance && orderPrice <= intentPrice * priceTolerance

    private def matchQuantity(orderQuantity: BigDecimal, intentQuantity: BigDecimal): Boolean =
      if (orderQuantity > 0)
        orderQuantity >= intentQuantity / quantityTolerance && orderQuantity <= intentQuantity * quantityTolerance
      else
        orderQuantity <= intentQuantity / quantityTolerance && orderQuantity >= intentQuantity * quantityTolerance
  }

}
