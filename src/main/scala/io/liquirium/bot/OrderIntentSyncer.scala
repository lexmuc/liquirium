package io.liquirium.bot

import io.liquirium.core.OperationIntent
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.Order.BasicOrderData

trait OrderIntentSyncer extends ((Seq[OrderIntent], Set[BasicOrderData]) => Iterable[OperationIntent])
