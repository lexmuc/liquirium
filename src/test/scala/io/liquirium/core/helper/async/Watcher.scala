package io.liquirium.core.helper.async

import akka.actor.typed._
import io.liquirium.core.helper.async.Watcher.Watch

object Watcher {

  case class Watch(actor: ActorRef[_])

}

class Watcher extends ExtensibleBehavior[Watch]() {
  var isTerminated = false
  var watching = false

  override def receive(ctx: TypedActorContext[Watch], msg: Watch): Behavior[Watch] = {
    if (watching) throw new RuntimeException("Watcher can only watch one actor")
    ctx.asScala.watch(msg.actor)
    watching = true
    Watcher.this
  }

  override def receiveSignal(ctx: TypedActorContext[Watch], msg: Signal): Behavior[Watch] = msg match {
    case Terminated(_) => isTerminated = true; Watcher.this
    case _ => Watcher.this
  }

}
