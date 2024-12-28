package io.liquirium.util.async.helpers

import io.liquirium.util.CancelHandle
import io.liquirium.util.async.Subscription

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.concurrent.ExecutionContext

class FakeSubscription[T] extends Subscription[T] {

  var cancelled: AtomicBoolean = new AtomicBoolean(false)
  var capturedOnUpdate: AtomicReference[Option[T => Unit]] =
    new AtomicReference[Option[T => Unit]](None)

  private val cancelHandle = new CancelHandle {
    override def cancel(): Unit = {
      cancelled.set(true)
    }
  }

  override def run(onUpdate: T => Unit)(implicit ec: ExecutionContext): CancelHandle = {
    capturedOnUpdate.getAndUpdate(x => {
      if (x.isDefined) {
        throw new RuntimeException("Fake subscription may be run only once")
      }
      Some(onUpdate)
    })
    cancelHandle
  }

  def emit(el: T): Unit = {
    capturedOnUpdate.get() match {
      case Some(onUpdate) => onUpdate(el)
      case None => throw new RuntimeException("FakeSubscription not run yet")
    }
  }

  def isRunning: Boolean = capturedOnUpdate.get().isDefined && !cancelled.get()

  def isCancelled: Boolean = cancelled.get()

}
