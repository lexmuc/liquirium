package io.liquirium.connect

import io.liquirium.core.{CandleHistoryLoader, CandleHistorySegment}

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}


class LiveCandleHistoryLoader(
  batchLoader: Instant => Future[CandleBatch],
  candleLength: Duration,
)(implicit val executionContext: ExecutionContext) extends CandleHistoryLoader {

  def load(start: Instant, end: Instant): Future[CandleHistorySegment] =
    completeSegment(CandleHistorySegment.empty(start, candleLength), end)
      .map(_.padUntil(end).truncate(end))

  private def completeSegment(historySegment: CandleHistorySegment, end: Instant): Future[CandleHistorySegment] =
      if (historySegment.end.plus(candleLength).isAfter(end)) {
        Future { historySegment }
      }
      else {
        batchLoader.apply(historySegment.end) flatMap { batch =>
          if (batch.start != historySegment.end)
            throw new RuntimeException("Candle batch start does not equal expected start")
          val newSegment = historySegment.extendWith(batch.toHistorySegment)
          batch.nextBatchStart match {
            // next batch start is redundant because it equals current end
            case Some(_) => completeSegment(newSegment, end)
            case _ => Future { newSegment }
          }
        }
      }

}
