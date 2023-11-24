package io.liquirium.util.akka

import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.{ExecutionContext, Future}

trait ActorSpawner {

  def spawnAsync[T](behavior: Behavior[T]): Future[ActorRef[T]]

  def spawnAsync[T](behavior: Behavior[T], name: String): Future[ActorRef[T]]

  def executionContext: ExecutionContext

}
