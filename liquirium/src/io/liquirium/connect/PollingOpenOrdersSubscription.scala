package io.liquirium.connect

import io.liquirium.core.Order
import io.liquirium.util.CancelHandle
import io.liquirium.util.async.{Scheduler, Subscription}

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

case class PollingOpenOrdersSubscription(
  loadOrders: () => Future[Set[Order]],
  pollingInterval: FiniteDuration,
  scheduler: Scheduler,
) extends Subscription[Set[Order]] {

  override def run(onUpdate: Set[Order] => Unit)(implicit ec: ExecutionContext): CancelHandle = {
    val cancelled = new AtomicBoolean(false)

    def handleResponse(
      response: Try[Set[Order]],
      currentOrders: Option[Set[Order]],
    ): Unit =
      if (!cancelled.get()) {
        response match {
          case Success(oo) =>
            scheduler.schedule(pollingInterval)(() => requestOrders(Some(oo)))
            if (!currentOrders.contains(oo)) {
              onUpdate(oo)
            }
          case Failure(_) =>
            scheduler.schedule(pollingInterval)(() => requestOrders(currentOrders))
        }
      }

    def requestOrders(currentOrders: Option[Set[Order]]): Unit = {
      if (!cancelled.get()) {
        loadOrders.apply()
          .onComplete(r => handleResponse(r, currentOrders))
      }
    }

    requestOrders(None)

    () => cancelled.set(true)
  }

}
