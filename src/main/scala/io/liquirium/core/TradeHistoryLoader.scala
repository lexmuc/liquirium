package io.liquirium.core

import java.time.Instant
import scala.concurrent.Future

trait TradeHistoryLoader {

  def loadHistory(start: Instant, maybeEnd: Option[Instant]): Future[TradeHistorySegment]

}
