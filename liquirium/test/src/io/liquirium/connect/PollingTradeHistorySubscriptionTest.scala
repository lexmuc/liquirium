package io.liquirium.connect

import io.liquirium.core.TradeHistorySegment
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment => segment}
import io.liquirium.core.helpers.async.{AsyncTestWithScheduler, FutureServiceMock}
import io.liquirium.util.async.helpers.SubscriberProbe
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.duration._

class PollingTradeHistorySubscriptionTest extends AsyncTestWithScheduler with TestWithMocks {

  private val tradeLoader =
    new FutureServiceMock[Instant => Future[TradeHistorySegment], TradeHistorySegment](_.apply(*))

  private var overlapStrategy: TradeUpdateOverlapStrategy = TradeUpdateOverlapStrategy.complete
  private var interval: FiniteDuration = 1.second
  private var initialSegment = segment(sec(0))()

  def makeSubscription(): PollingTradeHistorySubscription =
    PollingTradeHistorySubscription(
      initialSegment = initialSegment,
      loadSegment = tradeLoader.instance,
      pollingInterval = interval,
      updateOverlapStrategy = overlapStrategy,
      scheduler = scheduler,
    )

  private def subscribe(): SubscriberProbe[TradeHistorySegment] =
    SubscriberProbe.subscribeTo(makeSubscription())

  test("just creating a subscription does not trigger any request") {
    makeSubscription()
    tradeLoader.expectNoCall()
  }

  test("when running a subscription it immediately requests the trades from the update start") {
    overlapStrategy = _.start
    initialSegment = segment(sec(10))()
    subscribe()
    tradeLoader.verify.apply(sec(10))
  }

  test("it emits an extended segment once the update is received") {
    initialSegment = segment(sec(10))(trade(sec(11), "A"))
    val updateSegment = segment(sec(10))(trade(sec(11), "A"), trade(sec(16), "B"))

    val probe = subscribe()

    tradeLoader.completeNext(updateSegment)
    probe.expectElements(initialSegment.extendWith(updateSegment))
  }

  test("it requests new segments with the given interval which starts only when the last request completed") {
    interval = 10.seconds
    initialSegment = segment(sec(10))(trade(sec(11), "A"))

    subscribe()

    tradeLoader.completeNext(initialSegment)
    scheduler.advanceTime(9.seconds)
    tradeLoader.verifyTimes(1).apply(*)
    scheduler.advanceTime(1.second)
    tradeLoader.verifyTimes(2).apply(*)

    scheduler.advanceTime(2.seconds) // wait before answering request
    tradeLoader.completeNext(initialSegment)
    scheduler.advanceTime(9.seconds)
    tradeLoader.verifyTimes(2).apply(*)
    scheduler.advanceTime(1.second)
    tradeLoader.verifyTimes(3).apply(*)
  }

  test("the given overlap strategy is used at the beginning and for the following requests") {
    overlapStrategy = _.end.minus(secs(2))
    interval = 10.seconds
    initialSegment = segment(sec(10))(trade(sec(14), "A"))

    subscribe()

    tradeLoader.verify.apply(sec(12))
    tradeLoader.completeNext(segment(sec(12))(trade(sec(26), "B")))
    scheduler.advanceTime(10.seconds)
    tradeLoader.verify.apply(sec(24))
  }

  test("when updated segments are received the callback is invoked every time") {
    interval = 10.seconds
    initialSegment = segment(sec(10))()

    val probe = subscribe()

    val update1 = segment(sec(10))(trade(sec(11), "A"))
    tradeLoader.completeNext(update1)

    probe.expectElements(initialSegment.extendWith(update1))

    scheduler.advanceTime(10.seconds)

    val update2 = segment(sec(11))(trade(sec(11), "A"), trade(sec(12), "B"))
    tradeLoader.completeNext(update2)
    probe.expectElements(
      initialSegment.extendWith(update1),
      initialSegment.extendWith(update1).extendWith(update2),
    )
  }

  test("it does not emit an update if the segment is not changed by the update, except for the first time") {
    interval = 10.seconds
    initialSegment = segment(sec(10))(trade(sec(11), "A"))

    val probe = subscribe()

    tradeLoader.completeNext(initialSegment)
    probe.expectElements(initialSegment)

    scheduler.advanceTime(10.seconds)

    tradeLoader.completeNext(initialSegment)
    probe.expectElements(initialSegment)
  }

  test("simply requests the trades again after the interval when an error happens") {
    interval = 10.seconds

    subscribe()

    tradeLoader.failNext(ex(123))

    scheduler.advanceTime(9.seconds)
    tradeLoader.expectNoFurtherOpenRequests()
    scheduler.advanceTime(1.seconds)

    tradeLoader.completeNext(initialSegment)

    scheduler.advanceTime(10.seconds)
    tradeLoader.failNext(ex(123))

    scheduler.advanceTime(9.seconds)
    tradeLoader.expectNoFurtherOpenRequests()
    scheduler.advanceTime(1.seconds)
    tradeLoader.openRequestCount shouldBe 1
  }

  test("when retrying the first request it still counts as the first so the callback is invoked even if unchanged") {
    interval = 10.seconds
    initialSegment = segment(sec(0))(trade(sec(11), "A"))

    val probe = subscribe()

    tradeLoader.failNext(ex(123))
    probe.expectNoElements()

    scheduler.advanceTime(10.seconds)

    tradeLoader.completeNext(initialSegment)
    probe.expectElements(initialSegment)
  }

  test("for subsequent retries there must be a change for the callback to be called") {
    interval = 10.seconds
    initialSegment = segment(sec(0))(trade(sec(11), "A"))

    val probe = subscribe()

    tradeLoader.completeNext(initialSegment)
    probe.expectElements(initialSegment)

    scheduler.advanceTime(10.seconds)
    tradeLoader.failNext(ex(123))
    scheduler.advanceTime(10.seconds)
    tradeLoader.completeNext(initialSegment)

    probe.expectElements(initialSegment)

    val updateSegment = segment(sec(5))(trade(sec(11), "A"), trade(sec(12), "B"))
    scheduler.advanceTime(10.seconds)
    tradeLoader.failNext(ex(123))
    scheduler.advanceTime(10.seconds)
    tradeLoader.completeNext(updateSegment)

    probe.expectElements(initialSegment, initialSegment.extendWith(updateSegment))
  }

  test("the current segment is maintained also during retries") {
    interval = 10.seconds
    initialSegment = segment(sec(10))()
    val update1 = segment(sec(10))(trade(sec(11), "A"))
    val update2 = segment(sec(11))(trade(sec(11), "A"), trade(sec(12), "B"))

    val probe = subscribe()

    tradeLoader.completeNext(update1)

    scheduler.advanceTime(10.seconds)
    tradeLoader.failNext(ex(123))
    scheduler.advanceTime(10.seconds)
    tradeLoader.completeNext(update2)

    probe.expectElements(
      initialSegment.extendWith(update1),
      initialSegment.extendWith(update1).extendWith(update2),
    )
  }

  test("no more requests are made after the subscription has been cancelled") {
    interval = 10.seconds
    initialSegment = segment(sec(0))(trade(sec(11), "A"))

    val probe = subscribe()
    tradeLoader.completeNext(initialSegment)

    scheduler.advanceTime(5.seconds)
    probe.cancel()
    scheduler.advanceTime(15.seconds)
    tradeLoader.expectNoFurtherOpenRequests()
  }

  test("responses to open requests are ignored when the subscription has been cancelled") {
    interval = 10.seconds
    initialSegment = segment(sec(0))(trade(sec(11), "A"))

    val probe = subscribe()

    tradeLoader.completeNext(initialSegment)

    val update = segment(sec(0))(trade(sec(11), "A"), trade(sec(12), "B"))
    scheduler.advanceTime(12.seconds)
    probe.cancel()
    tradeLoader.completeNext(update)

    probe.expectElements(initialSegment)
  }

}
