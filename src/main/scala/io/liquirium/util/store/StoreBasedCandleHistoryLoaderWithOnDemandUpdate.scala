package io.liquirium.util.store

import io.liquirium.core.CandleHistorySegment

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class StoreBasedCandleHistoryLoaderWithOnDemandUpdate(
  baseStore: CandleHistoryStore,
  liveSegmentLoader: Instant => Future[CandleHistorySegment],
  overlapCandlesCount: Int,
)(implicit ec: ExecutionContext) extends CandleHistoryLoader {

  override def load(start: Instant, time: Instant): Future[CandleHistorySegment] =
    baseStore.load(start, time).flatMap { storedHistory =>
      if (time == storedHistory.end) {
        Future.successful(storedHistory)
      }
      else {
        val updateStart = storedHistory.dropRight(overlapCandlesCount).end
        liveSegmentLoader.apply(updateStart).flatMap { fullLiveHistory =>
          baseStore.updateHistory(fullLiveHistory).map { _ =>
            storedHistory.extendWith(fullLiveHistory.truncate(time))
          }
        }
      }
    }

}
