package io.liquirium.util.store

import io.liquirium.core.{TradeHistoryLoader, TradeHistorySegment}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class TradeHistoryStore(
  baseStore: TradeStore,
)(
  implicit executionContext: ExecutionContext,
) extends TradeHistoryLoader {

  def loadHistory(start: Instant, maybeEnd: Option[Instant]): Future[TradeHistorySegment] =
    baseStore.get(from = Some(start), until = maybeEnd) map { tb => tb.toHistorySegment }

  def updateHistory(historySegment: TradeHistorySegment): Future[Unit] =
    baseStore.deleteFrom(historySegment.start).flatMap { _ =>
      if (historySegment.nonEmpty) baseStore.add(historySegment)
      else Future { () }
    }

}
