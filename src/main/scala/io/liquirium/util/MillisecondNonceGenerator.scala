package io.liquirium.util

class MillisecondNonceGenerator(clock: Clock) extends NonceGenerator {

  var last: Long = 0

  val lock = new Object()

  override def next(): Long = lock.synchronized {
    val res = Math.max(last + 1, clock.getTime.toEpochMilli)
    last = res
    res
  }

}
