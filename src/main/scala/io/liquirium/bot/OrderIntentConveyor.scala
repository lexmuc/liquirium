package io.liquirium.bot

import io.liquirium.core.OperationIntent.{CancelIntent, OrderIntent}
import io.liquirium.core.Order.BasicOrderData
import io.liquirium.core.{Market, OperationIntent, OperationRequestId, OrderConstraints}
import io.liquirium.eval.Eval


object OrderIntentConveyor {

  def apply(
    market: Market,
    orderIntentsEval: Eval[Seq[OrderIntent]],
    orderConstraintsEval: Eval[OrderConstraints],
    openOrdersEval: Eval[Set[BasicOrderData]],
    orderSyncerEval: Eval[OrderIntentSyncer],
    nextMessageIdsEval: Eval[Stream[OperationRequestId]],
  ): Eval[Iterable[BotOutput]] = for {
    orderSyncer <- orderSyncerEval
    orderConstraints <- orderConstraintsEval
    orderIntents <- orderIntentsEval
    openOrders <- openOrdersEval
    nextMessageIds <- nextMessageIdsEval
  } yield {
    val adjustedIntents =
      orderIntents
        .map { oi => orderConstraints.adjustDefensively(oi) }
        .collect { case Some(x) => x }

    val operationIntents = orderSyncer.apply(adjustedIntents, openOrders)

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
