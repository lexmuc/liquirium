package io.liquirium.util.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.FiniteDuration

object AsyncRequestThrottler {

  sealed trait InternalProtocol

  final case class GotMessage[T](msg: T) extends InternalProtocol

  final case object Tick extends InternalProtocol

  def behavior[T](baseActor: ActorRef[T], minInterval: FiniteDuration): Behavior[T] = Behaviors.setup { context =>

    def internalBehavior(): Behavior[InternalProtocol] = Behaviors.withTimers { timers =>

      def sendNextAndStartTimer(queue: Seq[T]): Behavior[InternalProtocol] =
        queue.headOption match {
          case Some(msg) =>
            baseActor ! msg
            timers.startSingleTimer(Tick, Tick, minInterval)
            delaying(queue.tail)
          case None => idle()
        }

      def idle() = Behaviors.receiveMessagePartial[InternalProtocol] {
        case GotMessage(msg) => sendNextAndStartTimer(Seq(msg.asInstanceOf[T]))
      }

      def delaying(queue: Seq[T] = Seq()): Behavior[InternalProtocol] = Behaviors.receiveMessage {
        case GotMessage(msg) => delaying(queue :+ msg.asInstanceOf[T])
        case Tick => sendNextAndStartTimer(queue)
      }

      idle()
    }

    val internalActor = context.spawnAnonymous(internalBehavior())

    Behaviors.receiveMessage { msg => internalActor ! GotMessage(msg); Behaviors.same }

  }

}
