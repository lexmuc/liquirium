package io.liquirium.helpers

import io.liquirium.util.Clock

import java.time.Instant

class FakeClock(var time: Instant) extends Clock {

  val lock: Object = new Object

  override def getTime: Instant = lock.synchronized {
    time
  }

  def set(t: Instant): Unit = lock.synchronized {
    time = t
  }

}

object FakeClock {

  def apply(instant: Instant): FakeClock = new FakeClock(instant)

}