package io.liquirium.core.helper.async

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ActorTestKitBase, ManualTime, TestProbe}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{DeadLetter, SuppressedDeadLetter}
import akka.testkit.EventFilter
import com.typesafe.config.ConfigFactory
import io.liquirium.core.helper.BasicTest

import scala.concurrent.duration._

class AsyncTestWithControlledTime extends BasicTest {

  def logLevel = "WARNING"

  private val config = ConfigFactory.parseString(
    s"""akka.actor.default-dispatcher = { type = akka.testkit.CallingThreadDispatcherConfigurator }
      |akka.actor.testkit.typed.single-expect-default = 1s
      |akka.loglevel = $logLevel
      |akka.loggers = ["akka.testkit.TestEventListener"]
    """.stripMargin
  ).withFallback(ManualTime.config)

  val testKit = ActorTestKit(ActorTestKitBase.testNameFromCallStack(), config)

  def actorProbe[T](): TestProbe[T] = testKit.createTestProbe[T]("typed-probe")

  implicit val actorSystem = testKit.system

  implicit val untypedActorSystem = testKit.system.toClassic

  implicit val executionContext = actorSystem.executionContext

  private val manualTime: ManualTime = ManualTime()(testKit.system)

  def wait(duration: FiniteDuration): Unit = manualTime.timePasses(duration)

  def waitSeconds(n: Int): Unit = wait(n.seconds)

  def expectNoMessageFor(on: TestProbe[_]*): Unit = expectNoMessageFor(0.seconds, on: _*)

  private def expectNoMessageFor(duration: FiniteDuration, on: TestProbe[_]*): Unit =
    manualTime.expectNoMessageFor(duration, on: _*)

  def expectNoError[T](code: => T) = EventFilter.error(occurrences = 0).intercept(code)

  def expectNoWarning[T](code: => T) = EventFilter.warning(occurrences = 0).intercept(code)

  def expectNoErrorOrWarning[T](code: => T) = expectNoError(expectNoWarning(code))

  def expectNoDeadLetters[T](code: => T) = {
    // #TODO AKKA UPGRADE this should work with the typed actor system
    val deadLettersProbe = akka.testkit.TestProbe()
    val suppressedDeadLettersProbe = akka.testkit.TestProbe()
    untypedActorSystem.eventStream.subscribe(deadLettersProbe.ref, classOf[DeadLetter])
    untypedActorSystem.eventStream.subscribe(deadLettersProbe.ref, classOf[SuppressedDeadLetter])
    val res = code
    deadLettersProbe.expectNoMessage(0.seconds)
    suppressedDeadLettersProbe.expectNoMessage(0.seconds)
    res
  }

  val spawner = new UnsafeSpawnerWithWatchers(testKit)

}
