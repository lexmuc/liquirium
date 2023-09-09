package io.liquirium.core

import java.time.Instant
import scala.concurrent.Future

trait CandleHistoryLoader {

  def load(start: Instant, end: Instant): Future[CandleHistorySegment]

}
