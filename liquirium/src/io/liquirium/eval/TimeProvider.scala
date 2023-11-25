package io.liquirium.eval

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import io.liquirium.bot.BotInput.TimeInput
import io.liquirium.bot.{BotInput, BotInputProvider}
import io.liquirium.util.Clock
import io.liquirium.util.akka.ActorSpawner

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration


class TimeProvider(clock: Clock, spawner: ActorSpawner, queueBufferSize: Int) extends BotInputProvider {

  private sealed trait Protocol

  private case object EmitIfNextStepReached extends Protocol

  private def behavior(resolution: Duration, queue: SourceQueueWithComplete[Instant]): Behavior[Protocol] = {
    Behaviors.withTimers { timers =>
      Behaviors.setup { _ =>
        val resolutionMillis = resolution.toMillis
        def emitMillis(m: Long) = queue.offer(Instant.ofEpochMilli(m))

        val now = clock.getTime.toEpochMilli
        val initMillis = now / resolutionMillis * resolutionMillis

        emitMillis(initMillis)

        def scheduleNext(now: Long): Unit = {
          val delayUntilNext = FiniteDuration.apply(resolutionMillis - (now % resolutionMillis), TimeUnit.MILLISECONDS)
          timers.startSingleTimer(EmitIfNextStepReached, EmitIfNextStepReached, delayUntilNext)
        }

        scheduleNext(now)

        def running(lastMillis: Long): Behavior[Protocol] = Behaviors.receiveMessage {
          case EmitIfNextStepReached =>
            val now = clock.getTime.toEpochMilli
            val nextMillis = now - (now % resolutionMillis)
            if (nextMillis > lastMillis)
              emitMillis(nextMillis)
            scheduleNext(now)
            Behaviors.same
        }

        running(initMillis)
      }
    }

  }

  override def getInputUpdateStream[T](input: BotInput[T]): Option[Source[T, NotUsed]] = {
    input match {
      case TimeInput(resolution) => Some(
        Source.queue[Instant](queueBufferSize, OverflowStrategy.fail).mapMaterializedValue { q =>
          spawner.spawnAsync(behavior(resolution, q))
          NotUsed
        }.asInstanceOf[Source[T, NotUsed]]
      )
      case _ => None
    }

  }
}
