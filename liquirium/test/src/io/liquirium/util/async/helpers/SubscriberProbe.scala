package io.liquirium.util.async.helpers

import io.liquirium.util.async.Subscription
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.ExecutionContext


object SubscriberProbe {

  def subscribeTo[A](subscription: Subscription[A])(implicit ec: ExecutionContext): SubscriberProbe[A] = {
    new SubscriberProbe[A](subscription)
  }

}

class SubscriberProbe[A](subscription: Subscription[A])(implicit ec: ExecutionContext) {

  private val receivedElementsRef = new AtomicReference[List[A]](List())

  private val cancelHandler = subscription.run(e => receiveElement(e))

  private def receiveElement(e: A): Unit = {
    receivedElementsRef.updateAndGet(re => re :+ e)
  }

  def cancel(): Unit = cancelHandler.cancel()

  def receivedElements(): Seq[A] = receivedElementsRef.get()

  def expectElements(ee: A*): Unit = {
    receivedElements() shouldEqual ee.toList
  }

  def expectNoElements(): Unit = expectElements()

  def expectLatestElement(e: A): Unit = {
    receivedElements().last shouldEqual e
  }

}
