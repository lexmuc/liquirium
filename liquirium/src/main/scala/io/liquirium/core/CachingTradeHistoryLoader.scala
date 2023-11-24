package io.liquirium.core

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class CachingTradeHistoryLoader(
  baseLoader: TradeHistoryLoader,
  cache: TradeHistoryCache,
  overlap: Duration,
)(implicit val ec: ExecutionContext) extends TradeHistoryLoader {

  override def loadHistory(start: Instant, maybeEnd: Option[Instant]): Future[TradeHistorySegment] =
    updateCache(start).map { completeSegment =>
      val truncatedSegment = maybeEnd match {
        case Some(end) => completeSegment.truncate(end)
        case _ => completeSegment
      }
      truncatedSegment.takeFrom(start)
    }

  private def updateCache(start: Instant): Future[TradeHistorySegment] =
    cache.read().flatMap {
      case Some(cachedSegment) if !(cachedSegment.start isAfter start) =>
        loadNewTradesAndExtendCache(
          cachedSegment = cachedSegment,
          start = instantMax(cachedSegment.end.minus(overlap), cachedSegment.start),
        )
      case _ => completeReload(start)
    }

  private def loadNewTradesAndExtendCache(
    cachedSegment: TradeHistorySegment,
    start: Instant,
  ): Future[TradeHistorySegment] =
    baseLoader.loadHistory(start = start, maybeEnd = None) flatMap { extension =>
      cache.extendWith(extension).map(_ => {
        cachedSegment.extendWith(extension)
      })
    }

  private def completeReload(start: Instant): Future[TradeHistorySegment] =
    baseLoader.loadHistory(start, None) flatMap { loadedSegment =>
      cache.save(loadedSegment).map { _ => loadedSegment }
    }

  private def instantMax(a: Instant, b: Instant): Instant = if (a isAfter b) a else b

}
