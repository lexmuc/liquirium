package io.liquirium.util.store

import io.liquirium.core.CandleHistorySegment

import java.time.Instant
import scala.concurrent.Future

trait CandleHistoryLoader {

  def load(start: Instant, time: Instant): Future[CandleHistorySegment]

}
