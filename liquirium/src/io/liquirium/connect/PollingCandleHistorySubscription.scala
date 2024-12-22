package io.liquirium.connect

import io.liquirium.core.CandleHistorySegment
import io.liquirium.util.CancelHandle
import io.liquirium.util.async.{Scheduler, Subscription}

import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}


case class PollingCandleHistorySubscription(
  initialSegment: CandleHistorySegment,
  loadSegment: Instant => Future[CandleHistorySegment],
  pollingInterval: FiniteDuration,
  updateOverlapStrategy: CandleUpdateOverlapStrategy,
  scheduler: Scheduler,
) extends Subscription[CandleHistorySegment] {

  override def run(onUpdate: CandleHistorySegment => Unit)(implicit ec: ExecutionContext): CancelHandle = {
    val cancelled = new AtomicBoolean(false)

    def handleResponse(
      response: Try[CandleHistorySegment],
      currentSegment: CandleHistorySegment,
      firstRequest: Boolean,
    ): Unit =
      if (!cancelled.get()) {
        response match {
          case Success(chs) =>
            val newSegment = currentSegment.extendWith(chs)
            scheduler.schedule(pollingInterval)(() => requestCandles(newSegment, firstRequest = false))
            if (newSegment != currentSegment || firstRequest) {
              onUpdate(newSegment)
            }
          case Failure(_) =>
            scheduler.schedule(pollingInterval)(() => requestCandles(currentSegment, firstRequest = firstRequest))
        }
      }

    def requestCandles(currentSegment: CandleHistorySegment, firstRequest: Boolean): Unit = {
      if (!cancelled.get()) {
        loadSegment.apply(updateOverlapStrategy(currentSegment))
          .onComplete(r => handleResponse(r, currentSegment, firstRequest))
      }
    }

    requestCandles(initialSegment, firstRequest = true)

    () => cancelled.set(true)
  }

}
