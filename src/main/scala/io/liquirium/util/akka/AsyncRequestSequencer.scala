package io.liquirium.util.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import io.liquirium.util.akka.AsyncRequest.AsyncRequestMessage
import scala.concurrent.duration._

import scala.util.{Failure, Success, Try}

object AsyncRequestSequencer {

  sealed trait InternalProtocol[+T]

  final case class GotRequest[R, T <: AsyncRequest[R]](msg: AsyncRequestMessage[R, T]) extends InternalProtocol[T]

  final case class GotResponse[R, T](res: Try[R]) extends InternalProtocol[T]

  def forActor[T <: AsyncRequest[_]](baseActor: ActorRef[AsyncRequestMessage[_, T]])
  : Behavior[AsyncRequestMessage[_, T]] = Behaviors.setup { ctx =>

    def typed[CR, CT <: AsyncRequest[CR] with T](msg: AsyncRequestMessage[_, _]) =
      msg.asInstanceOf[AsyncRequestMessage[CR, CT]]

    def internalBehavior() = Behaviors.setup[InternalProtocol[T]] { internalContext =>

      def ask[CR, CT <: AsyncRequest[CR] with T](msg: AsyncRequestMessage[_, _]): Unit = {
        implicit val timeout: Timeout = 21474835.seconds
        internalContext.ask[AsyncRequestMessage[CR, CT], Try[CR]](baseActor, ref => typed(msg).changeReplyTo(ref)) {
          case Success(s) => GotResponse(s)
          case Failure(e) => GotResponse(Failure(e))
        }
      }

      def processNext(queue: Seq[AsyncRequestMessage[_, T]]): Behavior[InternalProtocol[T]] = queue.headOption match {
        case Some(msg) =>
          ask(msg)
          processing(msg.replyTo.asInstanceOf[ActorRef[Try[Any]]], queue.tail)
        case None => idle()
      }

      def processing(replyTo: ActorRef[Try[Any]], queue: Seq[AsyncRequestMessage[_, T]])
      : Behavior[InternalProtocol[T]] = Behaviors.receiveMessage {
        case GotRequest(msg) => processing(replyTo, queue :+ msg)
        case GotResponse(r) => replyTo ! r; processNext(queue)
      }

      def idle() = Behaviors.receiveMessagePartial[InternalProtocol[T]] {
        case GotRequest(msg) => processNext(Seq(msg))
      }

      idle()
    }

    val internalActor = ctx.spawnAnonymous(internalBehavior())

    Behaviors.receiveMessage { msg =>
      internalActor ! GotRequest(typed(msg))
      Behaviors.same
    }
  }

}
