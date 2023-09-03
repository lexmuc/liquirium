package io.liquirium.util.store

import io.liquirium.core.CandleHistorySegment

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CandleHistoryStore(
  baseStore: CandleStore,
)(
  implicit executionContext: ExecutionContext,
) {

  def loadHistory(start: Instant, inspectionTime: Option[Instant]): Future[CandleHistorySegment] =
    baseStore.get(from = Some(start), until = inspectionTime) map { cc =>
      val es = CandleHistorySegment.empty(start, baseStore.candleLength)
      if (cc.headOption.map(_.startTime) contains start) cc.foldLeft(es)(_.append(_)) else es
    }

  def updateHistory(historySegment: CandleHistorySegment): Future[Unit] =
    (if (historySegment.nonEmpty) baseStore.add(historySegment) else Future.successful(())) flatMap { _ =>
      baseStore.deleteFrom(historySegment.end)
    }

}
