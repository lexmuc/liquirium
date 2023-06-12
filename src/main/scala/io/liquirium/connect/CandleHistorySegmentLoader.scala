package io.liquirium.connect

import io.liquirium.core.CandleHistorySegment
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}


class CandleHistorySegmentLoader(
  batchLoader: Instant => Future[CandleBatch],
  dropLatest: Boolean,
)(implicit val executionContext: ExecutionContext) {

  def loadFrom(start: Instant): Future[CandleHistorySegment] =
    completeSegment(start, None).map { s =>
      if (dropLatest) s.dropRight(1) else s
    }

  def completeSegment(start: Instant, historySegment: Option[CandleHistorySegment]): Future[CandleHistorySegment] =
    batchLoader.apply(start) flatMap { batch =>
      if (batch.start != start)
        throw new RuntimeException("Candle batch start does not equal expected start")
      val segment = historySegment match {
        case None => batch.toHistorySegment
        case Some(seg) => seg.extendWith(batch.toHistorySegment)
      }
      batch.nextBatchStart match {
        case None => Future { segment }
        case Some(start) => completeSegment(start, Some(segment))
      }
    }

}
