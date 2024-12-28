package io.liquirium.bot

import io.liquirium.core.Order
import io.liquirium.core.orderTracking.{OpenOrdersHistory, OpenOrdersSnapshot}
import io.liquirium.util.{CancelHandle, Clock}
import io.liquirium.util.async.Subscription

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.concurrent.ExecutionContext

case class OpenOrdersHistorySubscription(
  openOrdersSubscription: Subscription[Set[Order]],
  clock: Clock,
) extends Subscription[OpenOrdersHistory]{

  override def run(onUpdate: OpenOrdersHistory => Unit)(implicit ec: ExecutionContext): CancelHandle = {
    val maybeHistory = new AtomicReference[Option[OpenOrdersHistory]](None)
    val isCancelled = new AtomicBoolean(false)

    val openOrdersCancelHandle = openOrdersSubscription.run{orderSet =>
      val snapshot = OpenOrdersSnapshot(orderSet, clock.getTime)
      val newHistory = maybeHistory.updateAndGet {
        case None => Some(OpenOrdersHistory.start(snapshot))
        case Some(h) => Some(h.appendIfChanged(snapshot))
      }
      if (!isCancelled.get())
        onUpdate(newHistory.get)
    }

    () => {
      isCancelled.set(true)
      openOrdersCancelHandle.cancel()
    }
  }

}
