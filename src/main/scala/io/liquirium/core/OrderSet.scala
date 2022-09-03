package io.liquirium.core

import scala.collection.SetLike

trait OrderSet extends Set[Order] with SetLike[Order, OrderSet] {

  def record(t: Trade): OrderSet

  def removeId(id: String): OrderSet

  override def empty: OrderSet = OrderSet.empty

}

object OrderSet {

  val empty: OrderSet = Impl(Set())

  def apply(oo: Set[Order]): OrderSet = Impl(oo)

  def apply(oo: Order*): OrderSet = Impl(oo.toSet)

  private case class Impl(orders: Set[Order]) extends OrderSet {
    if (orders.isInstanceOf[OrderSet])
      throw new RuntimeException("ExactOrder set should not be nested")

    override def record(t: Trade): OrderSet =
      t.orderId.map(x => orders.find(_.id == x)).flatten match {
        case None => this
        case Some(o) => copy((orders - o) ++ o.reduceQuantity(t.quantity))
      }

    override def contains(elem: Order): Boolean = orders(elem)

    override def +(elem: Order): OrderSet = copy(orders = orders + elem)

    override def -(elem: Order): OrderSet = copy(orders = orders - elem)

    override def iterator: Iterator[Order] = orders.iterator

    override def removeId(id: String): OrderSet = copy(orders = orders.filter(_.id != id))

  }

}
