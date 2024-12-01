package io.liquirium.core.orderTracking

import io.liquirium.core.Order

import java.time.Instant

case class OpenOrdersSnapshot(orders: Set[Order], timestamp: Instant) {

  private val ordersById: Map[String, Order] = orders.map(o => (o.id, o)).toMap

  def contains(id: String): Boolean = ordersById.contains(id)

  def get(id: String): Option[Order] = ordersById.get(id)

  val orderIds: Set[String] = ordersById.keySet

}
