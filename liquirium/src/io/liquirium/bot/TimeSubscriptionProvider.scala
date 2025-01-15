package io.liquirium.bot

import io.liquirium.bot.BotInput.TimeInput
import io.liquirium.eval.{Input, InputSubscriptionProvider}
import io.liquirium.util.{CancelHandle, Clock}
import io.liquirium.util.async.{Scheduler, Subscription}

import java.time.Instant
import scala.concurrent.ExecutionContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.FiniteDuration

class TimeSubscriptionProvider(
  scheduler: Scheduler,
  clock: Clock,
) extends InputSubscriptionProvider {

  override def apply(input: Input[_]): Option[Subscription[_]] = input match {
    case TimeInput(resolution) => Some(makeSubscription(resolution))

    case _ => None
  }

  def makeSubscription(resolution: Duration): Subscription[Instant] =
    new Subscription[Instant] {
      val isCancelled = new AtomicBoolean(false)
      val resolutionMillis: Long = resolution.toMillis
      override def run(onUpdate: Instant => Unit)(implicit ec: ExecutionContext): CancelHandle = {

        def emitMillis(m: Long): Unit = onUpdate(Instant.ofEpochMilli(m))

        val now = clock.getTime.toEpochMilli
        val initMillis = now / resolutionMillis * resolutionMillis

        emitMillis(initMillis)

        def emitIfNextStepReached(lastMillis: Long): Unit = {
          val now = clock.getTime.toEpochMilli
          val nextMillis = now - (now % resolutionMillis)
          if (nextMillis > lastMillis)
            emitMillis(nextMillis)
          scheduleNext(now, nextMillis)
        }

        def scheduleNext(now: Long, lastMillis: Long): Unit = {
          val delayUntilNext = FiniteDuration.apply(resolutionMillis - (now % resolutionMillis), TimeUnit.MILLISECONDS)
          scheduler.schedule(delayUntilNext) { () =>
            if (!isCancelled.get) {
              emitIfNextStepReached(lastMillis)
            }
          }
        }

        scheduleNext(now, initMillis)

        () => {
          isCancelled.set(true)
        }
      }
    }

}
