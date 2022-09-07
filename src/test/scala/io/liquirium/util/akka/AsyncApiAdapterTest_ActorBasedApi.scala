package io.liquirium.util.akka

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import io.liquirium.core.helper.CoreHelpers.ex
import io.liquirium.core.helper.TestWithMocks
import io.liquirium.core.helper.async.FutureServiceMock
import io.liquirium.helper.AsyncTestApi.{AsyncTestRequest, RequestA, RequestB}
import io.liquirium.helper.TypedActorTest
import io.liquirium.util.akka.AsyncRequest.AsyncRequestMessage

import scala.util.{Failure, Success, Try}

class AsyncApiAdapterTest_ActorBasedApi extends TypedActorTest with TestWithMocks {

  val baseApi = new FutureServiceMock[AsyncApi[AsyncTestRequest], Any](_.sendRequest(*))

  val apiActor: ActorRef[AsyncRequestMessage[_, AsyncTestRequest]] =
    testKit.spawn(AsyncApiAdapter.actorBasedApi(baseApi.instance))
  val aProbe: TestProbe[Try[Int]] = actorProbe[Try[Int]]()
  val bProbe: TestProbe[Try[Unit]] = actorProbe[Try[Unit]]()

  def a(n: Int): RequestA = RequestA(n.toString)
  def b(n: Int): RequestB = RequestB(n.toString)

  test("a request is forwarded to the future based api") {
    apiActor ! AsyncRequestMessage(a(123), aProbe.ref)
    baseApi.verify.sendRequest(a(123))
  }

  test("a success response is forwarded to the response recipient") {
    apiActor ! AsyncRequestMessage(a(123), aProbe.ref)
    baseApi.completeNext(123)
    aProbe.expectMessage(Success(123))
  }

  test("a failure is forwarded to the response recipient") {
    apiActor ! AsyncRequestMessage(a(123), aProbe.ref)
    baseApi.failNext(ex("123"))
    aProbe.expectMessage(Failure(ex("123")))
  }

  test("requests are processed independently") {
    apiActor ! AsyncRequestMessage(a(123), aProbe.ref)
    apiActor ! AsyncRequestMessage(b(123), bProbe.ref)
    val aPromise = baseApi.dequePromise()
    baseApi.failNext(ex("123"))
    bProbe.expectMessage(Failure(ex("123")))
    aPromise.complete(Success(123))
    aProbe.expectMessage(Success(123))
  }

}
