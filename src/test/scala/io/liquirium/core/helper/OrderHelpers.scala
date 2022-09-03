package io.liquirium.core.helper

import io.liquirium.core
import io.liquirium.core.OrderModifier.{FillOrKill, Margin, PostOnly}
import io.liquirium.core.helper.CoreHelpers.dec
import io.liquirium.core.helper.MarketHelpers.m
import io.liquirium.core.{Market, Order, OrderModifier, OrderSet}


object OrderHelpers {

  private val defaultMarket = m("x")

  def exactBuy(
    quantity: BigDecimal = dec(1),
    at: BigDecimal = dec(1),
    market: Market = defaultMarket,
    id: String = null
  ): Order = core.Order(
    id = if (id == null) s"buy${ quantity }at$at" else id,
    market = market,
    quantity = quantity,
    originalQuantity = quantity,
    price = at,
  )

  def exactSell(
    quantity: BigDecimal = dec(1),
                at: BigDecimal = dec(1),
                market: Market = defaultMarket,
                id: String = null,
  ): Order = core.Order(
      id = if (id == null) s"sell${ quantity }at$at" else id,
    market = market,
    quantity = -quantity,
    originalQuantity = -quantity,
    price = at
  )

  case class TestBasicOrderData(id: String, quantity: BigDecimal, price: BigDecimal) extends Order.BasicOrderData

  def basicOrderData(id: String, priceToQuantity: (BigDecimal, BigDecimal)): TestBasicOrderData =
    TestBasicOrderData(id, priceToQuantity._2, price = priceToQuantity._1)

  def orders(n: Int) = Set(order(n), order(n + 1))

  def order(id: String = "",
            quantity: BigDecimal = BigDecimal("1.0"),
            originalQuantity: BigDecimal = BigDecimal("1.0"),
            price: BigDecimal = BigDecimal("1.0"),
            market: Market = m(0)): Order =
    core.Order(
      id = id,
      market = market,
      quantity = quantity,
      originalQuantity = originalQuantity,
      price = price
    )

  def order(id: Int): Order = order(id.toString)

  def orders(oo: Order*): OrderSet = OrderSet(oo.toSet)

  def exactOrderWithAmountTuple(id: Int, amountAndFullAmount: (Int, Int) = (1, 1)): Order =
    order(
      id = id.toString,
      quantity = BigDecimal(amountAndFullAmount._1) / 10,
      originalQuantity = BigDecimal(amountAndFullAmount._2) / 10
    )

  implicit class FillStatusConverter(amount: Int) {
    def of(fullAmount: Int): (Int, Int) = (amount, fullAmount)
  }

  def modifiers(n: Int): Set[OrderModifier] = {
    val po: Option[OrderModifier] = if (n % 2 < 1) Some(PostOnly) else None
    val fok: Option[OrderModifier] = if (n % 4 < 2) Some(FillOrKill) else None
    val margin: Option[OrderModifier] = if (n % 8 < 4) Some(Margin) else None
    (po ++ fok ++ margin).toSet
  }

}
