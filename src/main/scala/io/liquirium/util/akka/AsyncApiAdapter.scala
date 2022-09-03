package io.liquirium.util.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import io.liquirium.util.akka.AsyncRequest.AsyncRequestMessage

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import akka.actor.typed.scaladsl.AskPattern._

object AsyncApiAdapter {

  def futureBasedApi[T <: AsyncRequest[_]](actor: ActorRef[AsyncRequestMessage[_, T]])
                                          (implicit system: ActorSystem[_]): AsyncApi[T] =
    new AsyncApi[T] {
      override def sendRequest[R](request: AsyncRequest[R] with T): Future[R] = {
        implicit val ec: ExecutionContextExecutor = system.executionContext
        implicit val timeout: Timeout = 21474835.seconds
        val f: Future[Try[R]] = actor.ask(ref => AsyncRequestMessage(request, ref))
        f.map {
          case Success(r) => r
          case Failure(e) => throw e
        }
      }
    }

  def actorBasedApi[T <: AsyncRequest[_]](api: AsyncApi[T]): Behavior[AsyncRequestMessage[_, T]] =
    Behaviors.receive { (ctx, msg) =>
      def sendRequest[R](request: AsyncRequest[_], replyTo: ActorRef[Try[R]]): Unit = {
        implicit val ec: ExecutionContextExecutor = ctx.executionContext
        api.sendRequest[R](request.asInstanceOf[T with AsyncRequest[R]]) onComplete { replyTo ! _ }
      }

      sendRequest(msg.request, msg.replyTo)
      Behaviors.same
    }

}
