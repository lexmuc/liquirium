package io.liquirium.util.async

import io.liquirium.util.CancelHandle

import scala.concurrent.ExecutionContext


trait Subscription[A] {

  def run(onUpdate: A => Unit)(implicit ec: ExecutionContext): CancelHandle

}
