package io.liquirium.util.store

import io.liquirium.core.TradeHistorySegment

import java.time.Instant
import scala.concurrent.Future

trait TradeHistoryLoader {

  def loadHistory(start: Instant, inspectionTime: Option[Instant]): Future[TradeHistorySegment]

}
