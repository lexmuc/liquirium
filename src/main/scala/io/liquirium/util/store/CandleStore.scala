package io.liquirium.util.store

import io.liquirium.core.Candle

import java.time.{Duration, Instant}
import scala.concurrent.Future

trait CandleStore {

  def candleLength: Duration

  def add(candles: Iterable[Candle]): Future[Unit]

  def get(from: Option[Instant] = None, until: Option[Instant] = None): Future[Iterable[Candle]]

  def getFirstStartAndLastEnd: Future[Option[(Instant, Instant)]]

  def clear(): Unit

  def deleteFrom(time: Instant): Future[Unit]

  def deleteBefore(time: Instant): Future[Unit]

}
