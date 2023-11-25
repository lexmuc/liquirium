package io.liquirium.bot

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import io.liquirium.bot.BotInput.{CompletedOperationRequest, CompletedOperationRequestsInSession}
import io.liquirium.bot.OperationRequestProcessor.UnavailableExchangeException
import io.liquirium.connect.ExchangeConnector
import io.liquirium.core.{ExchangeId, OperationRequest, OperationRequestSuccessResponse}
import io.liquirium.eval.IncrementalSeq
import io.liquirium.util.Clock
import io.liquirium.util.akka.ActorSpawner

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object OperationRequestProcessor {

  case class UnavailableExchangeException(exchangeId: ExchangeId, cause: Throwable) extends RuntimeException {
    override def getMessage: String = "Unavailable exchange: " + exchangeId.value + ": " + cause.getMessage

    override def getCause: Throwable = cause
  }

}

class OperationRequestProcessor(
  connectorsByExchangeId: ExchangeId => Future[ExchangeConnector],
  clock: Clock,
  spawner: ActorSpawner,
)(implicit val ec: ExecutionContext) extends BotInputProvider with BotOutputProcessor {

  sealed trait Protocol

  private final case class GotTradeRequestSuccessResponse(
    requestMessage: OperationRequestMessage,
    response: OperationRequestSuccessResponse[_ <: OperationRequest],
  ) extends Protocol

  private final case class GotTradeRequestFailure(requestMessage: OperationRequestMessage, t: Throwable) extends Protocol


  private val inputProviderPromise = Promise[CompletedTradeRequestsProvider]()
  private val actorFuture = spawner.spawnAsync(behavior())

  override def getInputUpdateStream[T](input: BotInput[T]): Option[Source[T, NotUsed]] =
    input match {
      case CompletedOperationRequestsInSession =>
        val source = Source.future(inputProviderPromise.future).flatMapConcat(_.getInputUpdateStream)
        Some(source)
      case _ => None
    }

  override def processOutput(output: BotOutput): Boolean = output match {
    case m: OperationRequestMessage =>
      connectorsByExchangeId.apply(m.request.market.exchangeId).onComplete {
        case Success(connector) => sendRequest(connector, m)
        case Failure(e) =>
          val uee = UnavailableExchangeException(m.request.market.exchangeId, e)
          actorFuture.foreach(_ ! GotTradeRequestFailure(m, uee))
      }
      true

    case _ => false
  }

  private def sendRequest(connector: ExchangeConnector, m: OperationRequestMessage): Unit =
    connector.submitRequest(m.request)
      .onComplete {
        case Success(res) =>
          actorFuture.foreach(_ ! GotTradeRequestSuccessResponse(m, res))
        case Failure(e) =>
          actorFuture.foreach(_ ! GotTradeRequestFailure(m, e))
      }

  type HistoryType = IncrementalSeq[CompletedOperationRequest]

  class CompletedTradeRequestsProvider(context: ActorContext[Protocol]) {
    implicit val system: ActorSystem[Nothing] = context.system

    var history: HistoryType = IncrementalSeq()
    val (sourceActor, broadcastSource) = ActorSource.actorRef[HistoryType](
      PartialFunction.empty,
      PartialFunction.empty,
      128,
      OverflowStrategy.fail
    ).toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both).run()
    broadcastSource.runWith(Sink.ignore)

    def getInputUpdateStream: Source[HistoryType, NotUsed] = Source.single(history).concat(broadcastSource)

    def addCompletedRequest(cr: CompletedOperationRequest): Unit = {
      history = history.inc(cr)
      sourceActor ! history
    }

  }

  def behavior(): Behavior[Protocol] =
    Behaviors.setup { context =>
      implicit val actorSystem: ActorSystem[Nothing] = context.system

      val completedTradeRequestsProvider = new CompletedTradeRequestsProvider(context)
      inputProviderPromise.success(completedTradeRequestsProvider)

      Behaviors.receiveMessage {

        case GotTradeRequestFailure(requestMessage, ex) =>
          completedTradeRequestsProvider.addCompletedRequest(
            CompletedOperationRequest(clock.getTime, requestMessage, Left(ex))
          )
          Behaviors.same

        case GotTradeRequestSuccessResponse(requestMessage, response) =>
          completedTradeRequestsProvider.addCompletedRequest(
            CompletedOperationRequest(clock.getTime, requestMessage, Right(response))
          )
          Behaviors.same

      }
    }

}
