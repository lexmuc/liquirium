package io.liquirium.core.orderTracking

import io.liquirium.core.orderTracking.BasicOrderTrackingState.SyncReason._
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalMapEval
import io.liquirium.eval.{Eval, IncrementalMap}

import java.time.{Duration, Instant}

object IsInSyncEval {

  def apply(
    statesByIdEval: Eval[IncrementalMap[String, BasicOrderTrackingState]],
    maxSyncDurationEval: Eval[Duration],
    currentTimeEval: Eval[Instant],
  ): Eval[Boolean] = {
    val syncingStatesEval = statesByIdEval.filterValuesIncremental(_.syncReasons.nonEmpty).map(_.mapValue.values)
    val errorStatesEval = statesByIdEval.filterValuesIncremental(_.errorState.isDefined).map(_.mapValue.values)
    for {
      maxDuration <- maxSyncDurationEval
      syncingStates <- syncingStatesEval
      errorStates <- errorStatesEval
      currentTime <- currentTimeEval
    } yield syncingStates.flatMap(_.syncReasons).collectFirst {
      case UnknownWhyOrderIsGone(reasonTime) if (reasonTime plus maxDuration).isAfter(currentTime) => ()
      case ExpectingObservationChange(_, _) => ()
      case ExpectingOrderToAppear(t, _) if (t plus maxDuration).isAfter(currentTime) => ()
      case ExpectingTrades(_, _) => ()
      case UnknownIfMoreTradesBeforeCancel(reasonTime) if (reasonTime plus maxDuration).isAfter(currentTime) => ()
    }.isEmpty && errorStates.isEmpty
  }

}
