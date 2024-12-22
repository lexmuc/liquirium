package io.liquirium.connect

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.FutureServiceMock
import io.liquirium.util.async.helpers.{FakeScheduler, SubscriberProbe}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

class PollingOpenOrdersSubscriptionTest extends TestWithMocks {

  private val orderLoader =
    new FutureServiceMock[() => Future[Set[Order]], Set[Order]](_.apply())

  private var interval: FiniteDuration = 1.second
  val scheduler = new FakeScheduler()

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor((runnable: Runnable) => runnable.run())

  def makeSubscription(): PollingOpenOrdersSubscription =
    PollingOpenOrdersSubscription(
      loadOrders = orderLoader.instance,
      pollingInterval = interval,
      scheduler = scheduler,
    )

  private def subscribe(): SubscriberProbe[Set[Order]] =
    SubscriberProbe.subscribeTo(makeSubscription())

  test("just creating a subscription does not trigger any request") {
    makeSubscription()
    orderLoader.expectNoCall()
  }

  test("when running a subscription it immediately requests the orders") {
    subscribe()
    orderLoader.verify.apply()
  }

  test("it emits the orders once they are received") {
    val probe = subscribe()

    orderLoader.completeNext(Set(order(1), order(2)))
    probe.expectElements(Set(order(1), order(2)))
  }

  test("it requests new segments with the given interval which starts only when the last request completed") {
    interval = 10.seconds

    subscribe()

    orderLoader.completeNext(Set())
    scheduler.advanceTime(9.seconds)
    orderLoader.verifyTimes(1).apply()
    scheduler.advanceTime(1.second)
    orderLoader.verifyTimes(2).apply()

    scheduler.advanceTime(2.seconds) // wait before answering request
    orderLoader.completeNext(Set())
    scheduler.advanceTime(9.seconds)
    orderLoader.verifyTimes(2).apply()
    scheduler.advanceTime(1.second)
    orderLoader.verifyTimes(3).apply()
  }

  test("when new, updated sets are received the callback is invoked every time") {
    interval = 10.seconds

    val probe = subscribe()

    orderLoader.completeNext(Set(order(1)))
    probe.expectElements(Set(order(1)))
    scheduler.advanceTime(10.seconds)

    orderLoader.completeNext(Set(order(2)))
    probe.expectElements(
      Set(order(1)),
      Set(order(2)),
    )
  }

  test("it does not emit an update if the orders have not changed but always for the first request") {
    interval = 10.seconds

    val probe = subscribe()

    orderLoader.completeNext(Set())
    probe.expectElements(Set())

    scheduler.advanceTime(10.seconds)

    orderLoader.completeNext(Set())
    probe.expectElements(Set())
  }

  test("simply requests the orders again after the interval when an error happens") {
    interval = 10.seconds

    subscribe()

    orderLoader.failNext(ex(123))

    scheduler.advanceTime(9.seconds)
    orderLoader.expectNoFurtherOpenRequests()
    scheduler.advanceTime(1.seconds)

    orderLoader.completeNext(Set())

    scheduler.advanceTime(10.seconds)
    orderLoader.failNext(ex(123))

    scheduler.advanceTime(9.seconds)
    orderLoader.expectNoFurtherOpenRequests()
    scheduler.advanceTime(1.seconds)
    orderLoader.openRequestCount shouldBe 1
  }

  test("the current set is maintained also during retries") {
    interval = 10.seconds

    val probe = subscribe()

    orderLoader.completeNext(Set(order(1)))

    scheduler.advanceTime(10.seconds)
    orderLoader.completeNext(Set(order(2)))

    scheduler.advanceTime(10.seconds)
    orderLoader.failNext(ex(123))
    scheduler.advanceTime(10.seconds)
    orderLoader.completeNext(Set(order(2)))

    probe.expectElements(
      Set(order(1)),
      Set(order(2)),
      // Set(order(2)) would be here again if internal current set had not been updated
    )
  }

  test("no more requests are made after the subscription has been cancelled") {
    interval = 10.seconds

    val probe = subscribe()
    orderLoader.completeNext(Set())

    scheduler.advanceTime(5.seconds)
    probe.cancel()
    scheduler.advanceTime(15.seconds)
    orderLoader.expectNoFurtherOpenRequests()
  }

  test("responses to open requests are ignored when the subscription has been cancelled") {
    interval = 10.seconds

    val probe = subscribe()

    orderLoader.completeNext(Set())

    scheduler.advanceTime(12.seconds)
    probe.cancel()
    orderLoader.completeNext(Set(order(1)))

    probe.expectElements(Set())
  }

}
