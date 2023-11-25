package io.liquirium.util.store

import io.liquirium.core.{TradeHistoryLoader, TradeHistorySegment}

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class StoreBasedTradeHistoryLoaderWithOnDemandUpdate(
  baseStore: TradeHistoryStore,
  liveSegmentLoader: Instant => Future[TradeHistorySegment],
  overlapDuration: Duration,
)(
  implicit ec: ExecutionContext,
) extends TradeHistoryLoader {

  override def loadHistory(start: Instant, maybeEnd: Option[Instant]): Future[TradeHistorySegment] =
    baseStore.loadHistory(start, maybeEnd).flatMap { storedHistory =>
      val updateStart = instantMax(storedHistory.end minus overlapDuration, start)

      liveSegmentLoader.apply(updateStart).flatMap { liveSegment =>
        baseStore.updateHistory(liveSegment).map { _ =>
          val fullNewSegment = storedHistory.extendWith(liveSegment)
          maybeEnd match {
            case Some(it) =>
              val tt = fullNewSegment.filter(_.time isBefore it)
              TradeHistorySegment.fromForwardTrades(start, tt)
            case _ => fullNewSegment
          }
        }
      }
    }

  private def instantMax(a: Instant, b: Instant): Instant = if (a.isAfter(b)) a else b

}
