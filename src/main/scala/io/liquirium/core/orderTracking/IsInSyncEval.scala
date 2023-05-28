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
  ): Eval[Boolean] =
    for {
      syncingStates <- statesByIdEval.filterValuesIncremental(_.syncReasons.nonEmpty).map(_.mapValue.values)
      errorStates <- statesByIdEval.filterValuesIncremental(_.errorState.isDefined).map(_.mapValue.values)
      maxDuration <- maxSyncDurationEval
      currentTime <- currentTimeEval
    } yield syncingStates.flatMap(_.syncReasons).collectFirst {
      case UnknownWhyOrderIsGone(reasonTime) if (reasonTime plus maxDuration).isAfter(currentTime) => ()
      case ExpectingObservationChange(_, _) => ()
      case ExpectingTrades(_, _) => ()
    }.isEmpty && errorStates.isEmpty

}
