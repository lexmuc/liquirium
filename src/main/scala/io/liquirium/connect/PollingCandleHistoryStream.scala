package io.liquirium.connect

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import io.liquirium.core.CandleHistorySegment
import io.liquirium.util.akka.SourceQueueFactory

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class PollingCandleHistoryStream(
  segmentLoader: Instant => Future[CandleHistorySegment],
  interval: FiniteDuration,
  retryInterval: FiniteDuration,
  updateOverlapStrategy: CandleUpdateOverlapStrategy,
  sourceQueueFactory: SourceQueueFactory,
) {

  private sealed trait Protocol

  private case class GotCandles(chs: CandleHistorySegment) extends Protocol

  private case object GetCandles extends Protocol

  private case object Completed extends Protocol

  private def streamBehavior(
    initialSegment: CandleHistorySegment,
    queue: SourceQueueWithComplete[CandleHistorySegment],
  ): Behavior[Protocol] =
    Behaviors.withTimers { timers =>
      Behaviors.setup[Protocol] { context =>
        implicit val executionContext: ExecutionContextExecutor = context.executionContext

        context.self ! GetCandles

        queue.watchCompletion().foreach(_ => context.self ! Completed)

        def running(segment: CandleHistorySegment): Behavior[Protocol] = Behaviors.receiveMessage {

          case GetCandles =>
            segmentLoader.apply(updateOverlapStrategy(segment)).onComplete {
              case Success(cc) => context.self ! GotCandles(cc)
              case Failure(_) =>
                timers.startSingleTimer(GetCandles, GetCandles, retryInterval)
            }
            Behaviors.same

          case GotCandles(cc) =>
            val extendedSegment = segment.extendWith(cc)
            queue.offer(extendedSegment)
            timers.startSingleTimer(GetCandles, GetCandles, interval)
            running(extendedSegment)

          case Completed =>
            Behaviors.stopped
        }

        running(initialSegment)
      }
    }

  def source(initialSegment: CandleHistorySegment): Source[CandleHistorySegment, NotUsed] =
    sourceQueueFactory.getWithActor(q => streamBehavior(initialSegment, q))

}
