package io.liquirium.bot

import io.liquirium.core.Order
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.helpers.async.AsyncTestWithScheduler
import io.liquirium.core.orderTracking.{OpenOrdersHistory, OpenOrdersSnapshot}
import io.liquirium.helpers.FakeClock
import io.liquirium.util.async.helpers.{FakeSubscription, SubscriberProbe}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class OpenOrdersHistorySubscriptionTest extends AsyncTestWithScheduler {

  private val openOrdersSubscription = new FakeSubscription[Set[Order]]
  private val clock = new FakeClock(sec(0))
  private val subscription = OpenOrdersHistorySubscription(openOrdersSubscription, clock)

  private def subscribe(): SubscriberProbe[OpenOrdersHistory] =
    SubscriberProbe.subscribeTo(subscription)

  test("it only runs the open orders subscription when it is run itself") {
    openOrdersSubscription.isRunning shouldBe false
    subscribe()
    openOrdersSubscription.isRunning shouldBe true
  }

  test("it returns the collected open order snapshot history with timestamps") {
    val probe = subscribe()
    clock.set(sec(1))
    openOrdersSubscription.emit(Set(order(1)))
    clock.set(sec(2))
    openOrdersSubscription.emit(Set(order(1), order(2)))
    clock.set(sec(3))
    openOrdersSubscription.emit(Set())

    val h1 = OpenOrdersHistory.start(
      OpenOrdersSnapshot(Set(order(1)), sec(1))
    )
    val h2 = h1.appendIfChanged(
      OpenOrdersSnapshot(Set(order(1), order(2)), sec(2))
    )
    val h3 = h2.appendIfChanged(
      OpenOrdersSnapshot(Set(), sec(3))
    )
    probe.expectElements(h1, h2, h3)
  }

  test("it cancels the open orders subscription when it is cancelled itself") {
    val probe = subscribe()
    probe.cancel()
    openOrdersSubscription.isCancelled shouldBe true
  }

  test("it does not produce any more elements if cancelled") {
    val probe = subscribe()
    clock.set(sec(1))
    openOrdersSubscription.emit(Set(order(1)))
    probe.cancel()
    clock.set(sec(2))
    openOrdersSubscription.emit(Set())

    val h1 = OpenOrdersHistory.start(
      OpenOrdersSnapshot(Set(order(1)), sec(1))
    )
    probe.expectElements(h1)
  }

}
