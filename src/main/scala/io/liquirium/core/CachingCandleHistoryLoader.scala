package io.liquirium.core

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class CachingCandleHistoryLoader(
  candleLength: Duration,
  baseLoader: CandleHistoryLoader,
  cache: CandleHistoryCache,
)(
  implicit ec: ExecutionContext
) extends CandleHistoryLoader {

  override def load(start: Instant, end: Instant): Future[CandleHistorySegment] = {
    cache.load(start, end).flatMap {
      case Right(cc) => Future.successful(cc)
      case Left((requestedStart, requestedEnd)) =>
        baseLoader.load(requestedStart, requestedEnd).flatMap(loadedCandles =>
          if (requestedEnd == end) {
            extendCacheAndQueryAgain(loadedCandles, start, loadedCandles.end)
          }
          else {
            extendCacheAndQueryAgain(loadedCandles, start, end)
          }
        )
    }
  }

  private def extendCacheAndQueryAgain(loadedCandles: CandleHistorySegment, start: Instant, end: Instant) = {
    cache.extendWith(loadedCandles).flatMap { _ =>
      cache.load(start, end).map {
        case Right(cc) => cc
        case Left(_) =>
          throw new RuntimeException("Unexpected candle cache behavior. It requested candles after an update.")
      }
    }
  }

}
