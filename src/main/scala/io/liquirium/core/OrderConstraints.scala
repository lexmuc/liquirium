package io.liquirium.core

import io.liquirium.core.OperationIntent.OrderIntent

case class OrderConstraints(
  pricePrecision: NumberPrecision,
  orderQuantityPrecision: NumberPrecision,
) {

  def adjustDefensively(orderIntent: OrderIntent): Option[OrderIntent] = {
    val isBuy = orderIntent.quantity.signum > 0
    val closestQuantity = orderQuantityPrecision.apply(orderIntent.quantity)
    val adjustedQuantity =
      if (closestQuantity.abs > orderIntent.quantity.abs) {
        if (isBuy) orderQuantityPrecision.nextLower(orderIntent.quantity)
        else orderQuantityPrecision.nextHigher(orderIntent.quantity)
      }
      else closestQuantity

    val closestPrice = pricePrecision.apply(orderIntent.price)
    val adjustedPrice =
      if (isBuy && closestPrice > orderIntent.price) pricePrecision.nextLower(orderIntent.price)
      else if (!isBuy && closestPrice < orderIntent.price) pricePrecision.nextHigher(orderIntent.price)
      else closestPrice

    if (adjustedPrice != BigDecimal(0) && adjustedQuantity != BigDecimal(0))
      Some(orderIntent.copy(
        quantity = adjustedQuantity,
        price = adjustedPrice,
      ))
    else None
  }

}
