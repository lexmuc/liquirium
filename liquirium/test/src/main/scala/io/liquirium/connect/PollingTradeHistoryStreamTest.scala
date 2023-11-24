package io.liquirium.connect

import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import io.liquirium.core.TradeHistorySegment
import io.liquirium.core.helpers.{TestWithMocks, TradeHelpers}
import TradeHelpers.{trade => t, tradeHistorySegment => segment}
import akka.stream.scaladsl.Sink
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.util.akka.SourceQueueFactory

import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.duration._

class PollingTradeHistoryStreamTest extends AsyncTestWithControlledTime with TestWithMocks {

  val tradeLoader = new FutureServiceMock[Instant => Future[TradeHistorySegment], TradeHistorySegment](_.apply(*))

  var updateOverlapStrategy: TradeUpdateOverlapStrategy = TradeUpdateOverlapStrategy.complete
  var interval: FiniteDuration = 1.second
  var retryInterval: FiniteDuration = 1.second

  private def stream() =
    new PollingTradeHistoryStream(
      segmentLoader = tradeLoader.instance,
      interval = interval,
      retryInterval = retryInterval,
      updateOverlapStrategy = updateOverlapStrategy,
      sourceQueueFactory = SourceQueueFactory.apply(spawner, bufferSize = 8, overflowStrategy = OverflowStrategy.fail),
    )

  private def runWithProbe(initialSegment: TradeHistorySegment): TestSubscriber.Probe[TradeHistorySegment] =
    stream().source(initialSegment).runWith(TestSink.probe[TradeHistorySegment])

  implicit val system: ActorSystem[Nothing] = actorSystem

  test("just obtaining a source does not trigger any request") {
    stream().source(segment(sec(10))())
    tradeLoader.expectNoCall()
    spawner.expectNoSpawn()
  }

  test("when running a source it immediately requests the trades from the update start") {
    updateOverlapStrategy = TradeUpdateOverlapStrategy.complete
    stream().source(segment(sec(10))()).runWith(Sink.ignore)
    tradeLoader.verify.apply(sec(10))
  }

  test("it emits an extended segment once the update is received") {
    val initialSegment = segment(sec(10))(t(sec(11), "A"))
    val sinkProbe = runWithProbe(initialSegment)
    val updateSegment = segment(sec(10))(t(sec(11), "B"), t(sec(12), "C"))
    tradeLoader.completeNext(updateSegment)
    sinkProbe.requestNext shouldEqual initialSegment.extendWith(updateSegment)
  }

  test("it requests the next segment for the new update start after the given interval (starting when received)") {
    updateOverlapStrategy = TradeUpdateOverlapStrategy.fixedOverlap(secs(0))
    interval = 10.seconds
    val initialSegment = segment(sec(10))(t(sec(11), "A"))
    runWithProbe(initialSegment)
    wait(2.seconds)
    val updateSegment = segment(sec(10))(t(sec(11), "B"), t(sec(12), "C"))
    tradeLoader.completeNext(updateSegment)
    wait(9.seconds)
    tradeLoader.expectNoFurtherOpenRequests()
    wait(1.seconds)
    tradeLoader.verifyTimes(2).apply(*)
    tradeLoader.verify.apply(sec(12))
  }

  test("it requests the candles again after an error after the given interval (starting after the error)") {
    updateOverlapStrategy = _.start
    interval = 100.seconds
    retryInterval = 10.seconds
    val initialSegment = segment(sec(10))(t(sec(11), "A"))
    runWithProbe(initialSegment)
    wait(2.seconds)
    tradeLoader.failNext(ex(123))
    wait(9.seconds)
    tradeLoader.expectNoFurtherOpenRequests()
    wait(1.seconds)
    tradeLoader.verifyTimes(2).apply(sec(10))
  }

  test("no more requests are made when the source has been cancelled") {
    interval = 100.seconds
    retryInterval = 10.seconds
    val initialSegment = segment(sec(10))(t(sec(11), "A"))
    val sinkProbe = runWithProbe(initialSegment)
    tradeLoader.failNext(ex(123))
    wait(2.seconds)
    sinkProbe.cancel()
    wait(9.seconds)
    tradeLoader.expectNoFurtherOpenRequests()
  }

}
