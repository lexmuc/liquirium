package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalMapEval
import io.liquirium.eval.{Eval, IncrementalMap}

object OpenOrdersBasedOnTrackingStates {

  def apply(statesByIdEval: Eval[IncrementalMap[String, BasicOrderTrackingState]]): Eval[Set[Order]] =
    statesByIdEval
      .filterValuesIncremental(_.reportingState.isDefined)
      .map { filteredValues =>
        val orders = filteredValues.mapValue.values.map(_.reportingState.get)
        orders.toIterator.toSet
      }

}
