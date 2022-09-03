package io.liquirium.core

import io.liquirium.core.OperationIntent.{CancelIntent, OrderIntent}

case class OperationIntentConverter(market: Market, orderModifiers: Set[OrderModifier])
  extends Function[Iterable[OperationIntent], Seq[OperationRequest]] {

  override def apply(operationIntents: Iterable[OperationIntent]): Seq[OperationRequest] = operationIntents.map {
    case ci: CancelIntent => ci.toOperationRequest(market)
    case oi: OrderIntent => oi.toOperationRequest(market, orderModifiers)
  }.toSeq

}
