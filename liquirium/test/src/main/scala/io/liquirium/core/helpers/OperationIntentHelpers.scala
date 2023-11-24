package io.liquirium.core.helpers

import io.liquirium.core.OperationIntent
import io.liquirium.core.OperationIntent.{CancelIntent, OrderIntent}
import io.liquirium.core.helpers.CoreHelpers.dec

object OperationIntentHelpers {

  def orderIntent(priceAndQuantity: (BigDecimal, BigDecimal)): OrderIntent =
    OrderIntent(quantity = priceAndQuantity._2, price = priceAndQuantity._1)

  def orderIntent(quantity: String, at: String): OrderIntent =
    OrderIntent(quantity = dec(quantity), price = dec(at))

  def orderIntent(n: Int): OrderIntent = orderIntent(BigDecimal(n) -> BigDecimal(n))

  def convenientOrderIntent(priceAndQuantity: (Int, Int)): OrderIntent =
    orderIntent((dec(priceAndQuantity._1), dec(priceAndQuantity._2)))

  def cancelIntent(id: String): CancelIntent = CancelIntent(id)

  def operationIntent(n: Int): OperationIntent =
    if (n % 2 == 0) orderIntent(n) else cancelIntent(n.toString)

}
