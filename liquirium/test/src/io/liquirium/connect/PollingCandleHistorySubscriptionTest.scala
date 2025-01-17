package io.liquirium.connect

import io.liquirium.core.CandleHistorySegment
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.CandleHelpers.{candleHistorySegment => segment}
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import io.liquirium.util.async.helpers.SubscriberProbe
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.duration._

class PollingCandleHistorySubscriptionTest extends AsyncTestWithScheduler with TestWithMocks {

  private val candleLoader =
    new FutureServiceMock[Instant => Future[CandleHistorySegment], CandleHistorySegment](_.apply(*))

  private var overlapStrategy: CandleUpdateOverlapStrategy = CandleUpdateOverlapStrategy.complete
  private var interval: FiniteDuration = 1.second
  private var initialSegment = CandleHistorySegment.empty(sec(0), secs(5))

  def makeSubscription(): PollingCandleHistorySubscription =
    PollingCandleHistorySubscription(
      initialSegment = initialSegment,
      loadSegment = candleLoader.instance,
      pollingInterval = interval,
      updateOverlapStrategy = overlapStrategy,
      scheduler = scheduler,
    )

  private def subscribe(): SubscriberProbe[CandleHistorySegment] =
    SubscriberProbe.subscribeTo(makeSubscription())

  test("just creating a subscription does not trigger any request") {
    makeSubscription()
    candleLoader.expectNoCall()
  }

  test("when running a subscription it immediately requests the candles from the update start") {
    overlapStrategy = _.start
    initialSegment = segment(sec(10), secs(5))()
    subscribe()
    candleLoader.verify.apply(sec(10))
  }

  test("it emits an extended segment once the update is received") {
    initialSegment = segment(sec(10), secs(5))(1, 2)
    val updateSegment = segment(sec(15), secs(5))(22, 33)

    val probe = subscribe()

    candleLoader.completeNext(updateSegment)
    probe.expectElements(initialSegment.extendWith(updateSegment))
  }

  test("it requests new segments with the given interval which starts only when the last request completed") {
    interval = 10.seconds
    initialSegment = segment(sec(10), secs(5))(1)

    subscribe()

    candleLoader.completeNext(initialSegment)
    scheduler.advanceTime(9.seconds)
    candleLoader.verifyTimes(1).apply(*)
    scheduler.advanceTime(1.second)
    candleLoader.verifyTimes(2).apply(*)

    scheduler.advanceTime(2.seconds) // wait before answering request
    candleLoader.completeNext(initialSegment)
    scheduler.advanceTime(9.seconds)
    candleLoader.verifyTimes(2).apply(*)
    scheduler.advanceTime(1.second)
    candleLoader.verifyTimes(3).apply(*)
  }

  test("the given overlap strategy is used at the beginning and for the following requests") {
    overlapStrategy = _.end.minus(secs(5))
    interval = 10.seconds
    initialSegment = segment(sec(10), secs(5))(1, 2)

    subscribe()

    candleLoader.verify.apply(sec(15))
    candleLoader.completeNext(segment(sec(15), secs(5))(2, 3, 4))
    scheduler.advanceTime(10.seconds)
    candleLoader.verify.apply(sec(25))
  }

  test("when updated segments are received the callback is invoked every time") {
    interval = 10.seconds
    initialSegment = segment(sec(10), secs(5))(1)

    val probe = subscribe()

    val update1 = segment(sec(10), secs(5))(1, 2)
    candleLoader.completeNext(update1)

    probe.expectElements(initialSegment.extendWith(update1))

    scheduler.advanceTime(10.seconds)

    val update2 = segment(sec(15), secs(5))(1, 4, 5)
    candleLoader.completeNext(update2)
    probe.expectElements(
      initialSegment.extendWith(update1),
      initialSegment.extendWith(update1).extendWith(update2),
    )
  }

  test("it does not emit an update if the segment is not changed by the update, except for the first time") {
    interval = 10.seconds
    initialSegment = segment(sec(10), secs(5))(1)

    val probe = subscribe()

    candleLoader.completeNext(initialSegment)
    probe.expectElements(initialSegment)

    scheduler.advanceTime(10.seconds)

    candleLoader.completeNext(initialSegment)
    probe.expectElements(initialSegment)
  }

  test("simply requests the candles again after the interval when an error happens") {
    interval = 10.seconds

    subscribe()

    candleLoader.failNext(ex(123))

    scheduler.advanceTime(9.seconds)
    candleLoader.expectNoFurtherOpenRequests()
    scheduler.advanceTime(1.seconds)

    candleLoader.completeNext(initialSegment)

    scheduler.advanceTime(10.seconds)
    candleLoader.failNext(ex(123))

    scheduler.advanceTime(9.seconds)
    candleLoader.expectNoFurtherOpenRequests()
    scheduler.advanceTime(1.seconds)
    candleLoader.openRequestCount shouldBe 1
  }

  test("when retrying the first request it still counts as the first so the callback is invoked even if unchanged") {
    interval = 10.seconds
    initialSegment = segment(sec(0), secs(5))(1, 2)

    val probe = subscribe()

    candleLoader.failNext(ex(123))
    probe.expectNoElements()

    scheduler.advanceTime(10.seconds)

    candleLoader.completeNext(initialSegment)
    probe.expectElements(initialSegment)
  }

  test("for subsequent retries there must be a change for the callback to be called") {
    interval = 10.seconds
    initialSegment = segment(sec(0), secs(5))(1, 2)

    val probe = subscribe()

    candleLoader.completeNext(initialSegment)
    probe.expectElements(initialSegment)

    scheduler.advanceTime(10.seconds)
    candleLoader.failNext(ex(123))
    scheduler.advanceTime(10.seconds)
    candleLoader.completeNext(initialSegment)

    probe.expectElements(initialSegment)

    val updateSegment = segment(sec(5), secs(5))(2, 3)
    scheduler.advanceTime(10.seconds)
    candleLoader.failNext(ex(123))
    scheduler.advanceTime(10.seconds)
    candleLoader.completeNext(updateSegment)

    probe.expectElements(initialSegment, initialSegment.extendWith(updateSegment))
  }

  test("the current segment is maintained also during retries") {
    interval = 10.seconds
    initialSegment = segment(sec(0), secs(5))(1)
    val update1 = segment(sec(5), secs(5))(2)
    val update2 = segment(sec(10), secs(5))(3)

    val probe = subscribe()

    candleLoader.completeNext(update1)

    scheduler.advanceTime(10.seconds)
    candleLoader.failNext(ex(123))
    scheduler.advanceTime(10.seconds)
    candleLoader.completeNext(update2)

    probe.expectElements(
      initialSegment.extendWith(update1),
      initialSegment.extendWith(update1).extendWith(update2),
    )
  }

  test("no more requests are made after the subscription has been cancelled") {
    interval = 10.seconds
    initialSegment = segment(sec(0), secs(5))(1)

    val probe = subscribe()
    candleLoader.completeNext(initialSegment)

    scheduler.advanceTime(5.seconds)
    probe.cancel()
    scheduler.advanceTime(15.seconds)
    candleLoader.expectNoFurtherOpenRequests()
  }

  test("responses to open requests are ignored when the subscription has been cancelled") {
    interval = 10.seconds
    initialSegment = segment(sec(0), secs(5))(1)

    val probe = subscribe()

    candleLoader.completeNext(initialSegment)

    val update = segment(sec(0), secs(5))(1, 2)
    scheduler.advanceTime(12.seconds)
    probe.cancel()
    candleLoader.completeNext(update)

    probe.expectElements(initialSegment)
  }

}
