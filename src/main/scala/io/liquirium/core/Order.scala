package io.liquirium.core

import io.liquirium.core.Order.BasicOrderData

/**
 * @param id           Order id uniquely identifying the order within its market
 * @param market       The market in which the order was placed
 * @param openQuantity Quantity that has not been filled yet (negative for sell orders)
 * @param fullQuantity Quantity when the order was created (negative for sell orders)
 * @param price        Price (for limit orders)
 */
final case class Order(
  id: String,
  market: Market,
  openQuantity: BigDecimal,
  fullQuantity: BigDecimal,
  price: BigDecimal,
) extends BasicOrderData {

  def resetQuantity: Order = copy(openQuantity = fullQuantity)

  def setQuantity(newQuantity: BigDecimal): Order = copy(openQuantity = newQuantity)

  def reduceQuantity(by: BigDecimal): Option[Order] =
    if (openQuantity == by) None else Some(copy(openQuantity = openQuantity - by))

  def volume: BigDecimal = openQuantity.abs * price

  def filledQuantity: BigDecimal = fullQuantity - openQuantity

}

object Order {

  trait BasicOrderData {
    def id: String

    def openQuantity: BigDecimal

    def price: BigDecimal

    def isBuy: Boolean = openQuantity.signum > 0

    def isSell: Boolean = !isBuy
  }

}

