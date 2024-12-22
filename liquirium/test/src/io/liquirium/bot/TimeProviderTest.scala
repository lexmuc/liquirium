package io.liquirium.bot

import akka.actor.typed.ActorSystem
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import io.liquirium.bot.BotInput.TimeInput
import io.liquirium.bot.helpers.BotInputHelpers.intInput
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.async.AsyncTestWithControlledTime
import io.liquirium.helpers.FakeClock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{FiniteDuration, _}

class TimeProviderTest extends AsyncTestWithControlledTime {

  implicit val system: ActorSystem[Nothing] = actorSystem

  val clock: FakeClock = FakeClock(sec(0))
  val provider = new TimeProvider(clock, spawner, 10)

  def advanceClockAndWait(duration: Duration): Unit = {
    clock.set(clock.getTime.plus(duration))
    wait(FiniteDuration(duration.toMillis, TimeUnit.MILLISECONDS))
  }

  def runAndGetProbe(resolution: Duration): TestSubscriber.Probe[Instant] = {
    val probe = provider.getInputUpdateStream(TimeInput(resolution)).get.runWith(TestSink.probe[Instant])
    probe.ensureSubscription()
    probe
  }

  test("it returns None for non-time inputs") {
    provider.getInputUpdateStream(intInput(1)) shouldEqual None
  }

  test("for time inputs it returns a source that immediately emits the last time with respect to the resolution") {
    clock.set(sec(28))
    val probe = runAndGetProbe(secs(10))
    probe.requestNext() shouldEqual sec(20)
  }

  test("subsequent instants are issued according to the given resolution at the correct time") {
    clock.set(sec(8))
    val probe = runAndGetProbe(secs(10))
    probe.request(10)
    probe.expectNext(sec(0))
    advanceClockAndWait(secs(1))
    probe.expectNoMessage()
    advanceClockAndWait(secs(1))
    probe.requestNext shouldEqual sec(10)
    advanceClockAndWait(secs(9))
    probe.expectNoMessage()
    advanceClockAndWait(secs(1))
    probe.expectNext(sec(20))
  }

  test("no additional timestamp is issued if the timer triggers too early, but it is later ") {
    clock.set(sec(8))
    val probe = runAndGetProbe(secs(10))
    probe.request(10)
    probe.expectNext(sec(0))
    clock.set(sec(9))
    wait(2.seconds)
    probe.expectNoMessage()
    clock.set(sec(10))
    wait(1.seconds)
    probe.expectNext(sec(10))
  }

  test("it skips timestamps if the timer is really late") {
    clock.set(sec(8))
    val probe = runAndGetProbe(secs(10))
    probe.request(10)
    probe.expectNext(sec(0))
    clock.set(sec(99))
    wait(2.seconds)
    probe.expectNext(sec(90))
  }

}
