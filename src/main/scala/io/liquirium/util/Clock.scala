package io.liquirium.util

import java.time.Instant

trait Clock {
  def getTime: Instant
}
