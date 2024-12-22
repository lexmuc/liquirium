package io.liquirium.connect

import io.liquirium.core.TradeHistorySegment
import io.liquirium.util.CancelHandle
import io.liquirium.util.async.{Scheduler, Subscription}

import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}


case class PollingTradeHistorySubscription(
  initialSegment: TradeHistorySegment,
  loadSegment: Instant => Future[TradeHistorySegment],
  pollingInterval: FiniteDuration,
  updateOverlapStrategy: TradeUpdateOverlapStrategy,
  scheduler: Scheduler,
) extends Subscription[TradeHistorySegment] {

  override def run(onUpdate: TradeHistorySegment => Unit)(implicit ec: ExecutionContext): CancelHandle = {
    val cancelled = new AtomicBoolean(false)

    def handleResponse(
      response: Try[TradeHistorySegment],
      currentSegment: TradeHistorySegment,
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

    def requestCandles(currentSegment: TradeHistorySegment, firstRequest: Boolean): Unit = {
      if (!cancelled.get()) {
        loadSegment.apply(updateOverlapStrategy(currentSegment))
          .onComplete(r => handleResponse(r, currentSegment, firstRequest))
      }
    }

    requestCandles(initialSegment, firstRequest = true)

    () => cancelled.set(true)
  }

}
