package io.liquirium.connect

import io.liquirium.core.CandleHistorySegment
import io.liquirium.util.store.CandleHistoryLoader

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}


class LiveCandleHistoryLoader(
  batchLoader: Instant => Future[CandleBatch],
  candleLength: Duration,
)(implicit val executionContext: ExecutionContext) extends CandleHistoryLoader {

  def load(start: Instant, time: Instant): Future[CandleHistorySegment] =
    completeSegment(CandleHistorySegment.empty(start, candleLength), time)
      .map(_.padUntil(time).truncate(time))

  private def completeSegment(historySegment: CandleHistorySegment, time: Instant): Future[CandleHistorySegment] =
      if (historySegment.end.plus(candleLength).isAfter(time)) {
        Future { historySegment }
      }
      else {
        batchLoader.apply(historySegment.end) flatMap { batch =>
          if (batch.start != historySegment.end)
            throw new RuntimeException("Candle batch start does not equal expected start")
          val newSegment = historySegment.extendWith(batch.toHistorySegment)
          batch.nextBatchStart match {
            // next batch start is redundant because it equals current end
            case Some(_) => completeSegment(newSegment, time)
            case _ => Future { newSegment }
          }
        }
      }

}
