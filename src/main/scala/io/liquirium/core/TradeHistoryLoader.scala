package io.liquirium.core

import java.time.Instant
import scala.concurrent.Future

trait TradeHistoryLoader {

  def loadHistory(start: Instant, inspectionTime: Option[Instant]): Future[TradeHistorySegment]

}
