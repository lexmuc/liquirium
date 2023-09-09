package io.liquirium.core

import io.liquirium.util.store.CandleStore

import java.time.Instant
import scala.concurrent.Future

class CachingCandleHistoryLoader(
  baseLoader: CandleHistoryLoader,
  store: CandleStore,
) extends CandleHistoryLoader {

  override def load(start: Instant, end: Instant): Future[CandleHistorySegment] = ???

}
