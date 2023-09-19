package io.liquirium.core

import io.liquirium.core.CandleHistoryCache.IncoherentCandleHistoryException
import io.liquirium.util.store.CandleStore

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}


object CandleHistoryCache {

  case class IncoherentCandleHistoryException(gapStart: Instant, gapEnd: Instant) extends Exception {
    override def getMessage: String = s"Gap in candle history: $gapStart - $gapEnd"
  }

}


class CandleHistoryCache(
  store: CandleStore,
)(implicit ec: ExecutionContext) {

  def load(start: Instant, end: Instant): Future[Either[(Instant, Instant), CandleHistorySegment]] =
    store.getFirstStartAndLastEnd.flatMap {

      case Some((storedStart, storedEnd)) if !(start isBefore storedStart) && !(end isAfter storedEnd) =>
        store.get(Some(start), Some(end)).map { candles =>
          Right(CandleHistorySegment.fromCandles(candles))
        }

      case Some((storedStart, storedEnd)) =>
        if (storedStart isBefore start) {
          request(
            start = storedEnd,
            end = if (end isAfter storedEnd) end else storedEnd,
          )
        }
        else if (storedStart == start) {
          request(
            start = storedEnd,
            end = end,
          )
        }
        else {
          request(
            start = start,
            end = if (end isAfter storedEnd) end else storedStart,
          )
        }

      case None => request(start, end)

    }

  private def request(start: Instant, end: Instant): Future[Either[(Instant, Instant), CandleHistorySegment]] =
    Future.successful(Left((start, end)))

  def extendWith(candleHistorySegment: CandleHistorySegment): Future[Unit] =
    store.getFirstStartAndLastEnd.flatMap {

      case Some((_, storedEnd)) if storedEnd isBefore candleHistorySegment.start =>
        throw IncoherentCandleHistoryException(storedEnd, candleHistorySegment.start)

      case Some((storedStart, _)) if storedStart isAfter candleHistorySegment.end =>
        throw IncoherentCandleHistoryException(candleHistorySegment.end, storedStart)

      case _ => store.add(candleHistorySegment)
    }

}
