package io.liquirium.util.async

import io.liquirium.util.CancelHandle

import scala.concurrent.duration._

trait Scheduler {

  def schedule(delay: FiniteDuration)(callback: () => Unit): CancelHandle

}

