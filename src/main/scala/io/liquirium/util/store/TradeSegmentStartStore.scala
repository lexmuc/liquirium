package io.liquirium.util.store

import java.time.Instant
import scala.concurrent.Future

trait TradeSegmentStartStore {

  def saveStart(start: Instant): Future[Unit]

  def readStart: Future[Option[Instant]]

}
