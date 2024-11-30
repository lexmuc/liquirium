package io.liquirium.core

object OrderSetOps {

  implicit class OrderSetOps(orderSet: Set[Order]) {

    def record(t: Trade): Set[Order] =
      t.orderId.flatMap(x => orderSet.find(_.id == x)) match {
        case None => orderSet
        case Some(o) =>
          val newOrder = o.reduceQuantity(t.quantity)
          if (newOrder.openQuantity == BigDecimal(0))
            orderSet - o
          else
            (orderSet - o) + newOrder
      }

  }

}
