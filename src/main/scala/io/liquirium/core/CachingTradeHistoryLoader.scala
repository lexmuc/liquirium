package io.liquirium.core
import java.time.{Duration, Instant}
import scala.concurrent.Future

class CachingTradeHistoryLoader(
  baseLoader: TradeHistoryLoader,
  cache: TradeHistoryCache,
  overlap: Duration,
) extends TradeHistoryLoader {

  override def loadHistory(start: Instant, maybeEnd: Option[Instant]): Future[TradeHistorySegment] = ???

}
