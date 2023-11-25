package io.liquirium.util.akka


import akka.actor.typed.ActorSystem

import scala.concurrent.ExecutionContext


trait ConcurrencyContext {

  def spawner: ActorSpawner

  def executionContext: ExecutionContext

  def sourceQueueFactory: SourceQueueFactory

  def asyncHttpService: AsyncHttpService

  val actorSystem: ActorSystem[_]

}
