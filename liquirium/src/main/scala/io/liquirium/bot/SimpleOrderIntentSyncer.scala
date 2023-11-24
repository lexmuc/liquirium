package io.liquirium.bot

import io.liquirium.core.OperationIntent
import io.liquirium.core.OperationIntent.{CancelIntent, OrderIntent}
import io.liquirium.core.Order.BasicOrderData

case class SimpleOrderIntentSyncer(matcher: OrderMatcher) extends OrderIntentSyncer {

  def apply(intents: Seq[OrderIntent], openOrders: Set[BasicOrderData]): Iterable[OperationIntent] =
    resultingIntents(intents.filter(_.quantity > 0), openOrders.filter(_.openQuantity > 0)) ++
      resultingIntents(intents.filter(_.quantity < 0), openOrders.filter(_.openQuantity < 0))

  private def resultingIntents(
    intents: Seq[OrderIntent],
    openOrders: Set[BasicOrderData],
  ): Iterable[OperationIntent] =
    if (openOrders.isEmpty) intents
    else if (openOrders.size == intents.size && unmatchedOrders(intents, openOrders).isEmpty) Seq()
    else cancelAll(openOrders)

  private def unmatchedOrders(intents: Seq[OrderIntent], openOrders: Set[BasicOrderData]) =
    intents.foldLeft(openOrders) { case (oo, i) => oo -- oo.find(o => matcher(o, i)) }

  private def cancelAll(openOrders: Set[BasicOrderData]) = openOrders.map(_.id).map(id => CancelIntent(id)).toSeq

}
