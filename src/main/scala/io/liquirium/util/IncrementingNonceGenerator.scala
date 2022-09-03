package io.liquirium.util

import java.util.concurrent.atomic.AtomicLong

class IncrementingNonceGenerator(val init: Long) extends NonceGenerator {

  val last = new AtomicLong(init)

  override def next(): Long = last.incrementAndGet()

}
