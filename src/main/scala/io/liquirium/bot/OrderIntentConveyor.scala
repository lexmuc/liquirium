package io.liquirium.bot

import io.liquirium.core.OperationIntent.{CancelIntent, OrderIntent}
import io.liquirium.core.Order.BasicOrderData
import io.liquirium.core.{Market, OperationIntent, OperationRequestId, Order, OrderConstraints}
import io.liquirium.eval.Eval


object OrderIntentConveyor {

  def apply(
    market: Market,
    orderConstraintsEval: Eval[OrderConstraints],
    orderIntentSyncer: Eval[OrderIntentSyncer],
    openOrdersEval: Eval[Set[Order]],
    isInSyncEval: Eval[Boolean],
    hasOpenRequestsEval: Eval[Boolean],
    nextMessageIdsEval: Eval[Stream[OperationRequestId]],
  ): Eval[Seq[OrderIntent] => Iterable[BotOutput]] =
    for {
      orderSyncer <- orderIntentSyncer
      orderConstraints <- orderConstraintsEval
      openOrders <- openOrdersEval
      isInSync <- isInSyncEval
      hasOpenRequests <- hasOpenRequestsEval
      nextMessageIds <- nextMessageIdsEval
    } yield
      (orderIntents: Seq[OrderIntent]) => {
        if (!isInSync || hasOpenRequests) Seq()
        else {
          val adjustedIntents =
            orderIntents
              .map { oi => orderConstraints.adjustDefensively(oi) }
              .collect { case Some(x) => x }

          val orderDataSet = openOrders.map(_.asInstanceOf[BasicOrderData])
          val operationIntents = orderSyncer.apply(adjustedIntents, orderDataSet)

          def convertIntent(oi: OperationIntent, id: OperationRequestId): OperationRequestMessage =
            oi match {
              case oi: OrderIntent => OperationRequestMessage(id, oi.toOperationRequest(market, Set()))
              case ci: CancelIntent => OperationRequestMessage(id, ci.toOperationRequest(market))
            }

          val (_, result) = operationIntents.foldLeft((nextMessageIds, Seq[OperationRequestMessage]())) {
            case ((ids, messages), intent) => (ids.drop(1), messages :+ convertIntent(intent, ids.head))
          }
          result
        }
      }

}
