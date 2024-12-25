package io.liquirium.core.helpers.async

import org.mockito.Mockito
import org.mockito.Mockito.{mock, times, inOrder => ordered}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.verification.VerificationMode
import org.scalatest.Assertions.fail

import scala.concurrent.{Future, Promise}
import scala.reflect.ClassTag
import scala.util.Success

class FutureServiceMock[T <: AnyRef, R]
(
  methodCall: T => Future[R],
  extendMock: Option[T] = None
)(
  implicit classTag: ClassTag[T]
) {

  private val mockee: T = extendMock getOrElse mock(classTag.runtimeClass).asInstanceOf[T]

  private var promises: Seq[Promise[R]] = Seq()
  private var responseQueue: Seq[R] = Seq()
  private var returnedFutures: Seq[Future[R]] = Seq()

  private lazy val order = ordered(mockee)

  init()

  def init(): Unit =
    Mockito.when(methodCall(mockee)).thenAnswer(_ => {
      val p = Promise[R]
      if (responseQueue.isEmpty) {
        promises = promises :+ p
      }
      else {
        p.success(responseQueue.head)
        responseQueue = responseQueue.tail
      }

      val f = p.future
      returnedFutures = returnedFutures :+ f
      f
    })

  def dequePromise(): Promise[R] = {
    val p = promises.head
    promises = promises.tail
    p
  }

  def openRequestCount(): Int = promises.size

  def enqueueResponse(response: R): Unit = {
    responseQueue = responseQueue :+ response
  }

  def instance: T = mockee

  def verifyNever: T = Mockito.verify(mockee, Mockito.never())

  def verify: T = Mockito.verify(mockee)

  def verifyTimes(n: Int): T = verifyWithMode(times(n))

  def verifyWithMode(m: VerificationMode): T = Mockito.verify(mockee, m)

  def verifyInOrder: T = order.verify(mockee)

  def expectNoCall(): Unit = methodCall(verifyNever)

  def expectCall(): Unit = methodCall(verify)

  def completeNext(result: R): Unit = {
    promises.head.complete(Success(result))
    promises = promises.tail
  }

  def lastReturnedFuture(): Future[R] = returnedFutures.last

  def reset(): Unit = {
    Mockito.reset(mockee)
    promises = Seq()
    responseQueue = Seq()
    init()
  }

  def expectNoFurtherOpenRequests(): Unit = if (promises.nonEmpty) fail("Expected no further open requests")

  def failNext(t: Throwable): Unit = {
    promises.head.failure(t)
    promises = promises.tail
  }

}
