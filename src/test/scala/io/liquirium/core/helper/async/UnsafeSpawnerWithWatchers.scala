package io.liquirium.core.helper.async

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, Behavior}
import io.liquirium.util.akka.ActorSpawner
import org.scalatest.Matchers

import scala.concurrent.{ExecutionContext, Future}

class UnsafeSpawnerWithWatchers(testKit: ActorTestKit) extends ActorSpawner with Matchers {

  implicit val executionContext: ExecutionContext = testKit.system.executionContext

  private var watchers: Seq[Watcher] = Seq()

  private var queuedFailures: Seq[Throwable] = Seq()

  private def failOrSpawn[T](spawn: () => ActorRef[T]): ActorRef[T] =
    queuedFailures.headOption match {
      case Some(f) =>
        queuedFailures = queuedFailures.tail
        throw f
      case None =>
        val actorRef = spawn()
        createWatcher(actorRef)
        actorRef
    }

  private def createWatcher[T](a: ActorRef[T]): Unit = {
    val w = new Watcher()
    testKit.spawn(w) ! Watcher.Watch(a)
    watchers = watchers :+ w
  }

  def enqueueFailure(t: Throwable): Unit = {
    queuedFailures = Seq(t)
  }

  def dequeueWatcher(): Watcher = {
    val res = watchers.head
    watchers = watchers.tail
    res
  }

  def expectNoSpawn(): Unit = {
    watchers shouldEqual Seq()
  }

  override def spawnAsync[T](behavior: Behavior[T]): Future[ActorRef[T]] =
    Future { failOrSpawn(() => testKit.spawn(behavior)) }

  override def spawnAsync[T](behavior: Behavior[T], name: String): Future[ActorRef[T]] =
    Future { failOrSpawn(() => testKit.spawn(behavior, name)) }

}
