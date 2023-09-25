package io.liquirium.connect

import io.liquirium.core.{TradeHistoryLoader, TradeHistorySegment}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class BatchBasedTradeHistoryLoader(
  batchLoader: Instant => Future[TradeBatch]
)(implicit val ec: ExecutionContext) extends TradeHistoryLoader {

  override def loadHistory(start: Instant, maybeEnd: Option[Instant]): Future[TradeHistorySegment] =
    appendNext(start, maybeEnd = maybeEnd, historySegment = None)

  private def appendNext(
    start: Instant,
    maybeEnd: Option[Instant],
    historySegment: Option[TradeHistorySegment],
  ): Future[TradeHistorySegment] =
    batchLoader.apply(start) flatMap { batch =>
      if (batch.start != start)
        throw new RuntimeException("Trade batch start does not equal expected start")
      val newSegment: TradeHistorySegment = extendSegment(
        currentSegment = historySegment,
        maybeEnd = maybeEnd,
        batch = batch,
      )
      if (containsTooLateTrade(batch, maybeEnd))
        Future {
          newSegment
        }
      else {
        batch.nextBatchStart match {
          case Some(start) if maybeEnd.isEmpty || start.isBefore(maybeEnd.get) =>
            appendNext(start, maybeEnd = maybeEnd, historySegment = Some(newSegment))
          case _ => Future {
            newSegment
          }
        }
      }
    }

  private def extendSegment(
    currentSegment: Option[TradeHistorySegment],
    batch: TradeBatch,
    maybeEnd: Option[Instant],
  ) =
    currentSegment match {
      case None => batch.toHistorySegment
      case Some(seg) =>
        val extension = maybeEnd match {
          case None => batch.toHistorySegment
          case Some(end) => batch.toHistorySegment.truncate(end)
        }
        seg.extendWith(extension)
    }

  private def containsTooLateTrade(batch: TradeBatch, maybeEnd: Option[Instant]): Boolean =
    maybeEnd match {
      case None => false
      case Some(end) => batch.trades.lastOption.exists(t => !t.time.isBefore(end))
    }

}
