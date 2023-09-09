package io.liquirium.util.store

import io.liquirium.core.CandleHistorySegment

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CandleHistoryStore(
  baseStore: CandleStore,
)(
  implicit executionContext: ExecutionContext,
) extends CandleHistoryLoader {

  def load(start: Instant, time: Instant): Future[CandleHistorySegment] =
    baseStore.get(from = Some(start), until = Some(time)) map { cc =>
      val es = CandleHistorySegment.empty(start, baseStore.candleLength)
      if (cc.headOption.map(_.startTime) contains start) cc.foldLeft(es)(_.append(_)) else es
    }

  def updateHistory(historySegment: CandleHistorySegment): Future[Unit] = {
    clearEarlierCandlesIfGapWouldBeFormed(historySegment) flatMap { _ =>
      (if (historySegment.nonEmpty) baseStore.add(historySegment) else Future.successful(())) flatMap { _ =>
        baseStore.deleteFrom(historySegment.end)
      }
    }
  }

  private def clearEarlierCandlesIfGapWouldBeFormed(newSegment: CandleHistorySegment): Future[Unit] =
    baseStore.get(
      from = Some(newSegment.start minus newSegment.candleLength),
      until = Some(newSegment.start),
    ).flatMap { cc =>
      if (cc.isEmpty) {
        baseStore.deleteBefore(newSegment.start)
      }
      else {
        Future.successful(())
      }
    }

}
