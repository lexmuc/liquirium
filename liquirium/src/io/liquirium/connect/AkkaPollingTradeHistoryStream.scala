package io.liquirium.connect

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import io.liquirium.core.TradeHistorySegment
import io.liquirium.util.akka.SourceQueueFactory

import java.time.Instant
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class AkkaPollingTradeHistoryStream(
  segmentLoader: Instant => Future[TradeHistorySegment],
  interval: FiniteDuration,
  retryInterval: FiniteDuration,
  updateOverlapStrategy: TradeUpdateOverlapStrategy,
  sourceQueueFactory: SourceQueueFactory,
) {

  private sealed trait Protocol

  private case class GotTrades(chs: TradeHistorySegment) extends Protocol

  private case object GetTrades extends Protocol

  private case object Completed extends Protocol

  private def streamBehavior(
    initialSegment: TradeHistorySegment,
    queue: SourceQueueWithComplete[TradeHistorySegment],
  ): Behavior[Protocol] =
    Behaviors.withTimers { timers =>
      Behaviors.setup[Protocol] { context =>
        implicit val executionContext: ExecutionContextExecutor = context.executionContext

        context.self ! GetTrades

        queue.watchCompletion().foreach(_ => context.self ! Completed)

        def running(segment: TradeHistorySegment): Behavior[Protocol] = Behaviors.receiveMessage {

          case GetTrades =>
            segmentLoader.apply(updateOverlapStrategy(segment)).onComplete {
              case Success(cc) => context.self ! GotTrades(cc)
              case Failure(_) =>
                timers.startSingleTimer(GetTrades, GetTrades, retryInterval)
              case _ => ()
            }
            Behaviors.same

          case GotTrades(tt) =>
            val extendedSegment = segment.extendWith(tt)
            queue.offer(extendedSegment)
            timers.startSingleTimer(GetTrades, GetTrades, interval)
            running(extendedSegment)

          case Completed =>
            Behaviors.stopped

        }

        running(initialSegment)
      }
    }

  def source(initialSegment: TradeHistorySegment): Source[TradeHistorySegment, NotUsed] =
    sourceQueueFactory.getWithActor(q => streamBehavior(initialSegment, q))

}
