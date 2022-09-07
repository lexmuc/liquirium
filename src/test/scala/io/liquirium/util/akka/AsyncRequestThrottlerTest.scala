package io.liquirium.util.akka

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import io.liquirium.helper.AsyncTestApi.{AsyncTestRequest, reqA}
import io.liquirium.helper.TypedActorTest
import io.liquirium.util.akka.AsyncRequest.AsyncRequestMessage

import scala.concurrent.duration.{FiniteDuration, _}

class AsyncRequestThrottlerTest extends TypedActorTest {

  val baseActor: TestProbe[AsyncRequestMessage[_, AsyncTestRequest]] =
    actorProbe[AsyncRequestMessage[_, AsyncTestRequest]]()

  private def msg(n: Int) = AsyncRequestMessage(reqA(n), null)

  var actor: ActorRef[AsyncRequestMessage[_, AsyncTestRequest]] = null

  private def init(minInterval: FiniteDuration = 1.second): Unit = {
    actor = testKit.spawn(AsyncRequestThrottler.behavior(baseActor.ref, minInterval))
  }

  test("the first request is immediately forwarded") {
    init()
    actor ! msg(1)
    baseActor expectMessage msg(1)
  }

  test("subsequent messages are held back until the given min interval has passed") {
    init(minInterval = 3.seconds)
    actor ! msg(1)
    actor ! msg(2)
    actor ! msg(3)
    baseActor expectMessage msg(1)
    wait(2.seconds)
    baseActor.expectNoMessage()
    wait(1.second)
    baseActor expectMessage msg(2)
    wait(2.seconds)
    baseActor.expectNoMessage()
    wait(1.second)
    baseActor expectMessage msg(3)
  }

  test("new messages are immediately forwared when the queue runs empty and the delay has passed") {
    init(minInterval = 3.seconds)
    actor ! msg(1)
    baseActor expectMessage msg(1)
    wait(3.seconds)
    actor ! msg(2)
    baseActor expectMessage msg(2)
    wait(4.seconds)
    actor ! msg(3)
    baseActor expectMessage msg(3)
  }

}
