package io.liquirium.util.akka

import io.liquirium.core.helper.CoreHelpers.ex
import io.liquirium.helper.AsyncTestApi.{AsyncTestRequest, RequestA, RequestB}
import io.liquirium.helper.TypedActorTest
import io.liquirium.util.akka.AsyncRequest.AsyncRequestMessage

import scala.util.{Failure, Success}

class AsyncApiAdapterTest_FutureBasedApi extends TypedActorTest {

  private val baseActor = actorProbe[AsyncRequestMessage[_, AsyncTestRequest]]()
  private val api = AsyncApiAdapter.futureBasedApi[AsyncTestRequest](baseActor.ref)

  private def a(n: Int) = RequestA(n.toString)
  private def b(n: Int) = RequestB(n.toString)

  test("a request is forwarded") {
    api.sendRequest(a(1))
    baseActor.expectMessageType[AsyncRequestMessage[_, RequestA]].request shouldEqual a(1)
  }

  test("a success response completes the future") {
    val f = api.sendRequest(a(1))
    val replyTo = baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]].replyTo
    replyTo ! Success(123)
    f.value.get shouldEqual Success(123)
  }

  test("a failure fails the future") {
    val f = api.sendRequest(a(1))
    val replyTo = baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]].replyTo
    replyTo ! Failure(ex("123"))
    f.value.get shouldEqual Failure(ex("123"))
  }

  test("requests are processed independently") {
    val fA = api.sendRequest(a(1))
    val fB = api.sendRequest(b(2))
    val replyToA = baseActor.expectMessageType[AsyncRequestMessage[Int, RequestA]].replyTo
    val replyToB = baseActor.expectMessageType[AsyncRequestMessage[Unit, RequestB]].replyTo
    replyToB ! Failure(ex("123"))
    replyToA ! Success(123)
    fA.value.get shouldEqual Success(123)
    fB.value.get shouldEqual Failure(ex("123"))
  }

}
