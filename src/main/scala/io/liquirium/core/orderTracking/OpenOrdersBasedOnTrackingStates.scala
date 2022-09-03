package io.liquirium.core.orderTracking

import io.liquirium.core.OrderSet
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalMapMetric
import io.liquirium.eval.{Eval, IncrementalMap}

object OpenOrdersBasedOnTrackingStates {

  def apply(statesByIdEval: Eval[IncrementalMap[String, BasicOrderTrackingState]]): Eval[OrderSet] =
    statesByIdEval
      .filterValuesIncremental(_.reportingState.isDefined)
      .map { filteredValues =>
        val orders = filteredValues.mapValue.values.map(_.reportingState.get)
        OrderSet(orders.toSet)
      }

}
