package io.liquirium.bot.simulation

import io.liquirium.eval.Input

import java.time.Instant

trait SingleInputUpdateStreamProvider {

  def getInputStream(input: Input[_], start: Instant, end: Instant): Option[LazyList[(Instant, Any)]]

}
