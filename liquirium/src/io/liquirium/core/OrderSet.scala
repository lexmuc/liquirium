package io.liquirium.core

import scala.collection.immutable.SetOps
import scala.collection.{IterableFactory, immutable, mutable}


// Custom OrderSet class
case class OrderSet private(
  private val orders: immutable.Set[Order]
) extends immutable.Set[Order]
  with immutable.SetOps[Order, immutable.Set, OrderSet] {

  // Required methods for SetOps
  override def contains(elem: Order): Boolean = orders.contains(elem)

  override def iterator: Iterator[Order] = orders.iterator

  override def incl(elem: Order): OrderSet = new OrderSet(orders + elem)

  override def excl(elem: Order): OrderSet = new OrderSet(orders - elem)

  def record(t: Trade): OrderSet =
    t.orderId.flatMap(x => orders.find(_.id == x)) match {
      case None => this
      case Some(o) =>
        val newOrder = o.reduceQuantity(t.quantity)
        if (newOrder.openQuantity == BigDecimal(0))
          copy(orders = orders - o)
        else
          copy((orders - o) + newOrder)
    }

  def removeId(id: String): OrderSet = copy(orders = orders.filter(_.id != id))

  // Ensure the companion factory methods work correctly
  override protected def fromSpecific(coll: IterableOnce[Order]): OrderSet =
    new OrderSet(coll.iterator.toSet)

  override protected def newSpecificBuilder: scala.collection.mutable.Builder[Order, OrderSet] =
    new scala.collection.mutable.Builder[Order, OrderSet] {
      private val innerBuilder = immutable.Set.newBuilder[Order]

      def clear(): Unit = innerBuilder.clear()

      def result(): OrderSet = new OrderSet(innerBuilder.result())

      override def addOne(elem: Order): this.type = {
        innerBuilder.addOne(elem)
        this
      }
    }

  override def empty: OrderSet = OrderSet.empty

  override def iterableFactory: IterableFactory[Set] = ???
}

// Companion object for factory methods
object OrderSet {
  def empty: OrderSet = new OrderSet(immutable.Set.empty)

  def apply(orders: Set[Order]): OrderSet = new OrderSet(orders)
}


//trait OrderSet extends Set[Order] with SetOps[Order, Set, OrderSet] {
//
//  def record(t: Trade): OrderSet
//
//  def removeId(id: String): OrderSet
//
//  override def empty: OrderSet = OrderSet.empty
//}
//
//object OrderSet {
//
//  val empty: OrderSet = Impl(Set.empty)
//
//  def apply(oo: Set[Order]): OrderSet = Impl(oo)
//
//  def apply(oo: Order*): OrderSet = Impl(oo.toSet)
//
//  private case class Impl(orders: Set[Order]) extends OrderSet {
//    if (orders.isInstanceOf[OrderSet])
//      throw new RuntimeException("OrderSet should not be nested")
//
//    override def record(t: Trade): OrderSet =
//      t.orderId.flatMap(x => orders.find(_.id == x)) match {
//        case None => this
//        case Some(o) =>
//          val newOrder = o.reduceQuantity(t.quantity)
//          if (newOrder.openQuantity == BigDecimal(0))
//            copy(orders = orders - o)
//          else
//            copy((orders - o) + newOrder)
//      }
//
//    override def contains(elem: Order): Boolean = orders.contains(elem)
//
////    override def +(elem: Order): OrderSet = copy(orders = orders + elem)
////
////    override def -(elem: Order): OrderSet = copy(orders = orders - elem)
//
//    override def iterator: Iterator[Order] = orders.iterator
//
//    override def removeId(id: String): OrderSet = copy(orders = orders.filter(_.id != id))
//
//    override def foreach[U](f: Order => U): Unit = orders.foreach(f)
//
//    override def empty: OrderSet = OrderSet.empty
//
//    override def incl(elem: Order): OrderSet = OrderSet(orders + elem)
//
//    override def excl(elem: Order): OrderSet = OrderSet(orders - elem)
//
////    override protected def coll: OrderSet = this
//
////    override protected def fromSpecific(coll: IterableOnce[Order]): OrderSet = OrderSet(coll.iterator.toSet)
//
//    override protected def newSpecificBuilder: mutable.Builder[Order, OrderSet] =
//      new mutable.Builder[Order, OrderSet] {
//        private val builder = Set.newBuilder[Order]
//
//        override def clear(): Unit = builder.clear()
//
//        override def result(): OrderSet = OrderSet(builder.result())
//
////        override def +=(elem: Order): this.type = {
////          builder += elem
////          this
////        }
//      }
//
//    override def iterableFactory: IterableFactory[Set] = ???
//  }
//}

