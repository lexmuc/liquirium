package io.liquirium.core.helpers

import io.liquirium.core.{Order, OrderSet, Trade}

case class FakeOrderSet(orders: Set[Order] = Set(), recordedTrades: Seq[Trade] = Seq()) extends OrderSet {

  override def record(t: Trade): FakeOrderSet = copy(recordedTrades = recordedTrades :+ t)

  override def removeId(id: String): FakeOrderSet = copy(orders = orders.filter(_.id != id))

  override def contains(elem: Order): Boolean = orders(elem)

  override def +(elem: Order): FakeOrderSet = copy(orders = orders + elem)

  override def -(elem: Order): FakeOrderSet = copy(orders = orders - elem)

  override def iterator: Iterator[Order] = orders.iterator

  override def equals(that: Any): Boolean = that match {
    case tos: FakeOrderSet => orders == tos.orders && recordedTrades == tos.recordedTrades
    case _ => super.equals(that)
  }

  override def toString: String = s"FakeOrderSet($orders, $recordedTrades)"

}
