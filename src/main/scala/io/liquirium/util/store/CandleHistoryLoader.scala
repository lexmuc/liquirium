package io.liquirium.util.store

import io.liquirium.core.CandleHistorySegment

import java.time.Instant
import scala.concurrent.Future

trait CandleHistoryLoader {

  def loadHistory(start: Instant, inspectionTime: Option[Instant]): Future[CandleHistorySegment]

}