package io.liquirium.util.async

import io.liquirium.util.CancelHandle

import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import scala.concurrent.duration.FiniteDuration


class ProductionScheduler extends Scheduler {

  private val scheduler = Executors.newScheduledThreadPool(1)

  override def schedule(delay: FiniteDuration)(callback: () => Unit): CancelHandle = {
    val scheduledTask: ScheduledFuture[_] = scheduler.schedule(
      new Runnable {
        override def run(): Unit = callback()
      },
      delay.toMillis,
      TimeUnit.MILLISECONDS
    )

    () => scheduledTask.cancel(false)
  }

  def shutdown(): Unit = scheduler.shutdown()

}
