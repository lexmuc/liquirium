package io.liquirium.util.store

import io.liquirium.core.TradeHistorySegment

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class StoreBasedTradeHistoryProviderWithOnDemandUpdate(
  baseStore: TradeHistoryStore,
  liveSegmentLoader: Instant => Future[TradeHistorySegment],
  overlapDuration: Duration,
)(
  implicit ec: ExecutionContext,
) extends TradeHistoryProvider {

  override def loadHistory(start: Instant, inspectionTime: Option[Instant]): Future[TradeHistorySegment] =
    baseStore.loadHistory(start, inspectionTime).flatMap { storedHistory =>
      val updateStart = instantMax(storedHistory.end minus overlapDuration, start)

      liveSegmentLoader.apply(updateStart).map { liveSegment =>
        baseStore.updateHistory(liveSegment)
        val fullNewSegment = storedHistory.extendWith(liveSegment)
        inspectionTime match {
          case Some(it) =>
            val tt = fullNewSegment.filter(_.time isBefore it)
            TradeHistorySegment.fromForwardTrades(start, tt)
          case _ => fullNewSegment
        }
      }
    }

  private def instantMax(a: Instant, b: Instant): Instant = if (a.isAfter(b)) a else b

}
