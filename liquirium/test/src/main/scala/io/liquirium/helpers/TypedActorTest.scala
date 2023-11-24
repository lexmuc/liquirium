package io.liquirium.helpers

import akka.actor.typed.ActorRef
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, Watcher}
import org.scalatest.BeforeAndAfterEach

abstract class TypedActorTest extends AsyncTestWithControlledTime with BeforeAndAfterEach {

  def behaviorProbe[T]() = new SynchronousBehaviorProbe[T](testKit)

  override def afterEach(): Unit = {
    super.afterEach()
    testKit.shutdownTestKit()
  }

  def attachWatcher[T](actorRef: ActorRef[T]): Watcher = {
    val w = new Watcher()
    testKit.spawn(w) ! Watcher.Watch(actorRef)
    w
  }

}
