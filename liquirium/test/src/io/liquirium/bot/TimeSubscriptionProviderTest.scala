package io.liquirium.bot

import io.liquirium.bot.BotInput.TimeInput
import io.liquirium.bot.helpers.BotInputHelpers.intInput
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.async.AsyncTestWithScheduler
import io.liquirium.helpers.FakeClock
import io.liquirium.util.async.Subscription
import io.liquirium.util.async.helpers.SubscriberProbe
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TimeSubscriptionProviderTest extends AsyncTestWithScheduler {

  val clock: FakeClock = FakeClock(sec(0))
  val provider = new TimeSubscriptionProvider(scheduler, clock)

  def advanceClockAndWait(duration: Duration): Unit = {
    clock.set(clock.getTime.plus(duration))
    scheduler.advanceTime(FiniteDuration(duration.toMillis, TimeUnit.MILLISECONDS))
  }

  def runAndGetProbe(resolution: Duration): SubscriberProbe[Instant] = {
    SubscriberProbe.subscribeTo(
      provider.apply(TimeInput(resolution)).get.asInstanceOf[Subscription[Instant]]
    )
  }

  test("it returns None for non-time inputs") {
    provider.apply(intInput(1)) shouldEqual None
  }

  test("for time inputs it returns a subscription that immediately emits the last time with correct resolution") {
    clock.set(sec(28))
    val probe = runAndGetProbe(secs(10))
    probe.expectElements(sec(20))
  }

  test("subsequent instants are issued according to the given resolution at the correct time") {
    clock.set(sec(8))
    val probe = runAndGetProbe(secs(10))
    probe.expectElements(sec(0))
    advanceClockAndWait(secs(1))
    probe.expectElements(sec(0))
    advanceClockAndWait(secs(1))
    probe.expectElements(sec(0), sec(10))
    advanceClockAndWait(secs(9))
    probe.expectElements(sec(0), sec(10))
    advanceClockAndWait(secs(1))
    probe.expectElements(sec(0), sec(10), sec(20))
  }

  test("no additional instant is issued if the timer triggers too early, but it is later") {
    clock.set(sec(8))
    val probe = runAndGetProbe(secs(10))
    probe.expectElements(sec(0))
    clock.set(sec(9))
    scheduler.advanceTime(2.seconds)
    probe.expectElements(sec(0))
    clock.set(sec(10))
    scheduler.advanceTime(1.second)
    probe.expectElements(sec(0), sec(10))
  }

  test("it skips timestamps if the timer is really late") {
    clock.set(sec(8))
    val probe = runAndGetProbe(secs(10))
    probe.expectElements(sec(0))
    clock.set(sec(99))
    scheduler.advanceTime(2.seconds)
    probe.expectElements(sec(0), sec(90))
  }

  test("no more elements are issued when the subscription is cancelled") {
    clock.set(sec(8))
    val probe = runAndGetProbe(secs(10))
    probe.expectElements(sec(0))
    probe.cancel()
    scheduler.advanceTime(2.seconds)
    probe.expectElements(sec(0))
  }

}
