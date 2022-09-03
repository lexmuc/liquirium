package io.liquirium.core

sealed trait OperationRequest {
  def market: Market
}

case class CancelRequest(market: Market, orderId: String) extends OperationRequest

case class OrderRequest(
  market: Market,
  quantity: BigDecimal,
  price: BigDecimal,
  modifiers: Set[OrderModifier],
) extends OperationRequest {

  if (quantity.signum == 0) throw new RuntimeException("An order request cannot have quantity 0.")

  def isBuy: Boolean = quantity.signum == 1

  def isSell: Boolean = !isBuy

  def volume: BigDecimal = quantity * price

  def toExactOrder(id: String): Order = Order(id, market, quantity, quantity, price = price)

  def changeQuantity(q: BigDecimal): OrderRequest = copy(quantity = q)

  def setModifiers(mods: Set[OrderModifier]): OrderRequest = copy(modifiers = mods)

}