package io.liquirium.util.akka

import akka.actor.typed.ActorRef

import scala.util.Try

trait AsyncRequest[+T]

object AsyncRequest {

  case class AsyncRequestMessage[R, +AR <: AsyncRequest[R]](request: AR, replyTo: ActorRef[Try[R]]) {

    def changeReplyTo(newReplyTo: ActorRef[Try[R]]): AsyncRequestMessage[R, AR] = copy(replyTo = newReplyTo)

  }

}


