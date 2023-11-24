package io.liquirium.core

import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.util.NumberPrecision


case class OrderConstraints(
  pricePrecision: NumberPrecision,
  quantityPrecision: NumberPrecision,
) {

  def adjustDefensively(orderIntent: OrderIntent): Option[OrderIntent] = {
    val isBuy = orderIntent.quantity.signum > 0
    val closestQuantity = quantityPrecision.apply(orderIntent.quantity)
    val adjustedQuantity =
      if (closestQuantity.abs > orderIntent.quantity.abs) {
        if (isBuy) quantityPrecision.nextLower(orderIntent.quantity)
        else quantityPrecision.nextHigher(orderIntent.quantity)
      }
      else closestQuantity

    val closestPrice = pricePrecision.apply(orderIntent.price)
    val adjustedPrice =
      if (isBuy && closestPrice > orderIntent.price) pricePrecision.nextLower(orderIntent.price)
      else if (!isBuy && closestPrice < orderIntent.price) pricePrecision.nextHigher(orderIntent.price)
      else closestPrice

    if (adjustedPrice == BigDecimal(0) || adjustedQuantity == BigDecimal(0)) None
    else Some(
      orderIntent.copy(
        quantity = adjustedQuantity,
        price = adjustedPrice,
      )
    )
  }

}
