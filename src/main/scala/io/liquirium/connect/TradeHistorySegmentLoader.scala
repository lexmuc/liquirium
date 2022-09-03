package io.liquirium.connect

import io.liquirium.core.TradeHistorySegment

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class TradeHistorySegmentLoader(
  batchLoader: Instant => Future[TradeBatch]
)(implicit val ec: ExecutionContext) {

  def loadFrom(start: Instant): Future[TradeHistorySegment] = appendNext(start, None)

  def appendNext(start: Instant, historySegment: Option[TradeHistorySegment]): Future[TradeHistorySegment] =
    batchLoader.apply(start) flatMap { batch =>
      if (batch.start != start)
        throw new RuntimeException("Trade batch start does not equal expected start")
      val segment = historySegment match {
        case None => batch.toHistorySegment
        case Some(seg) => seg.extendWith(batch.toHistorySegment)
      }
      batch.nextBatchStart match {
        case None => Future { segment }
        case Some(start) => appendNext(start, Some(segment))
      }
    }

}
