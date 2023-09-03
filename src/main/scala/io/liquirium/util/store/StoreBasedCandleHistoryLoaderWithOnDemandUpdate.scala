package io.liquirium.util.store

import io.liquirium.core.CandleHistorySegment

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class StoreBasedCandleHistoryLoaderWithOnDemandUpdate(
  baseStore: CandleHistoryStore,
  liveSegmentLoader: Instant => Future[CandleHistorySegment],
  overlapCandlesCount: Int,
)(implicit ec: ExecutionContext) extends CandleHistoryLoader {

  override def loadHistory(start: Instant, inspectionTime: Option[Instant]): Future[CandleHistorySegment] =
    baseStore.loadHistory(start, inspectionTime).flatMap { storedHistory =>
      if (inspectionTime.contains(storedHistory.end)) {
        Future.successful(storedHistory)
      }
      else {
        val updateStart = storedHistory.dropRight(overlapCandlesCount).end
        liveSegmentLoader.apply(updateStart).map { fullLiveHistory =>
          baseStore.updateHistory(fullLiveHistory)
          val maybeTruncatedLiveHistory = inspectionTime match {
            case Some(it) => fullLiveHistory.truncate(it)
            case None => fullLiveHistory
          }
          storedHistory.extendWith(maybeTruncatedLiveHistory)
        }
      }
    }

}
