package io.liquirium.util.async.helpers

import io.liquirium.util.CancelHandle
import io.liquirium.util.async.Scheduler

import scala.collection.mutable
import scala.concurrent.duration._

class FakeScheduler extends Scheduler {

  private case class Task(executionTime: Long, callback: () => Unit, isCancelled: () => Boolean)

  private var currentTime: Long = 0
  private val tasks = mutable.PriorityQueue.empty[Task](
    Ordering.by(-_.executionTime) // Execute tasks in order of their execution time
  )

  override def schedule(delay: FiniteDuration)(callback: () => Unit): CancelHandle = {
    val executionTime = currentTime + delay.toMillis
    var cancelled = false

    tasks.enqueue(Task(executionTime, callback, () => cancelled))

    () => cancelled = true
  }

  def advanceTime(duration: FiniteDuration): Unit = {
    val targetTime = currentTime + duration.toMillis
    while (tasks.nonEmpty && tasks.head.executionTime <= targetTime) {
      val task = tasks.dequeue()
      if (!task.isCancelled()) {
        currentTime = task.executionTime
        task.callback()
      }
    }
    currentTime = targetTime
  }

}
