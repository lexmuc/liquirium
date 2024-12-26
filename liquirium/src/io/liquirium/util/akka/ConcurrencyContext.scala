package io.liquirium.util.akka


import akka.actor.typed.ActorSystem
import io.liquirium.util.HttpService
import io.liquirium.util.async.Scheduler

import scala.concurrent.ExecutionContext


trait ConcurrencyContext {

  def spawner: ActorSpawner

  def executionContext: ExecutionContext

  def sourceQueueFactory: SourceQueueFactory

  def asyncHttpService: HttpService

  val actorSystem: ActorSystem[_]

  val scheduler: Scheduler

}
