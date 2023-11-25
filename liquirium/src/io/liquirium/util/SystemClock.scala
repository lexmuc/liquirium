package io.liquirium.util

import java.time.Instant

object SystemClock extends Clock {

  def getTime: Instant = Instant.now

}
