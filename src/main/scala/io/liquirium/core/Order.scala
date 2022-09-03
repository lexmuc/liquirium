package io.liquirium.core

import io.liquirium.core.Order.BasicOrderData

/**
 * @param id               Order id uniquely identifying the order within its market
 * @param market           The market in which the order was placed
 * @param quantity         Quantity is negative for sell orders
 * @param originalQuantity Quantity when the order was created
 * @param price            Price (for limit orders)
 */
final case class Order(
  id: String,
  market: Market,
  quantity: BigDecimal,
  originalQuantity: BigDecimal,
  price: BigDecimal,
) extends BasicOrderData {

  def resetQuantity: Order = copy(quantity = originalQuantity)

  def setQuantity(newQuantity: BigDecimal): Order = copy(quantity = newQuantity)

  def reduceQuantity(by: BigDecimal): Option[Order] =
    if (quantity == by) None else Some(copy(quantity = quantity - by))

  def volume: BigDecimal = quantity.abs * price

  def filledQuantity: BigDecimal = originalQuantity - quantity

}

object Order {

  trait BasicOrderData {
    def id: String

    def quantity: BigDecimal

    def price: BigDecimal

    def isBuy: Boolean = quantity.signum > 0

    def isSell: Boolean = !isBuy
  }

}

