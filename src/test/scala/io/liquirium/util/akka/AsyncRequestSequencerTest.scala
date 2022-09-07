package io.liquirium.util.akka

import io.liquirium.core.helper.CoreHelpers.ex
import io.liquirium.helper.AsyncTestApi._
import io.liquirium.helper.TypedActorTest
import io.liquirium.util.akka.AsyncRequest.AsyncRequestMessage

import scala.util.{Failure, Success, Try}

class AsyncRequestSequencerTest extends TypedActorTest {

  private val baseActor = actorProbe[AsyncRequestMessage[_, AsyncTestRequest]]()
  private val sequencer = testKit.spawn(AsyncRequestSequencer.forActor(baseActor.ref))

  private val probeA = actorProbe[Try[Int]]()
  private val probeB = actorProbe[Try[Unit]]()

  test("the first request is simply forwarded") {
    sequencer ! AsyncRequestMessage(reqA(123), probeA.ref)
    baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]].request shouldEqual reqA(123)
  }

  test("the subsequent requests are only forwarded once the respective previous ones are completed") {
    sequencer ! AsyncRequestMessage(reqA(123), probeA.ref)
    sequencer ! AsyncRequestMessage(reqB(123), probeB.ref)
    sequencer ! AsyncRequestMessage(reqA(123), probeA.ref)
    val msgA1 = baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]]
    baseActor.expectNoMessage()
    msgA1.replyTo ! Success(123)
    val msgB = baseActor.expectMessageType[AsyncRequestMessage[Unit, RequestB]]
    baseActor.expectNoMessage()
    msgB.replyTo ! Success(())
    baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]]
  }

  test("request failure also counts as completion") {
    sequencer ! AsyncRequestMessage(reqA(123), probeA.ref)
    sequencer ! AsyncRequestMessage(reqB(123), probeB.ref)
    val msgA = baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]]
    baseActor.expectNoMessage()
    msgA.replyTo ! Failure(ex(123))
    baseActor.expectMessageType[AsyncRequestMessage[Unit, RequestB]]
  }

  test("success and failure are delivered to the respective recipients") {
    sequencer ! AsyncRequestMessage(reqA(123), probeA.ref)
    sequencer ! AsyncRequestMessage(reqA(123), probeA.ref)
    baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]].replyTo ! Success(123)
    baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]].replyTo ! Failure(ex(123))
    probeA expectMessage Success(123)
    probeA expectMessage Failure(ex(123))
  }

  test("the next message after an empty queue is immediately forwarded") {
    sequencer ! AsyncRequestMessage(reqA(123), probeA.ref)
    val msgA = baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]]
    msgA.replyTo ! Failure(ex(123))
    sequencer ! AsyncRequestMessage(reqB(123), probeB.ref)
    baseActor.expectMessageType[AsyncRequestMessage[Unit, RequestB]]
  }

}
