package io.liquirium.connect

import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Sink
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import io.liquirium.core.CandleHistorySegment
import io.liquirium.core.helper.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helper.TestWithMocks
import io.liquirium.core.helper.CandleHelpers.{candleHistorySegment => segment}
import io.liquirium.core.helper.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.util.akka.SourceQueueFactory

import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.duration._


class PollingCandleHistoryStreamTest extends AsyncTestWithControlledTime with TestWithMocks {

  val candleLoader = new FutureServiceMock[Instant => Future[CandleHistorySegment], CandleHistorySegment](_.apply(*))

  var updateStartProvider: CandleHistorySegment => Instant = s => s.start
  var interval: FiniteDuration = 1.second
  var retryInterval: FiniteDuration = 1.second

  private def stream() =
    new PollingCandleHistoryStream(
      segmentLoader = candleLoader.instance,
      interval = interval,
      retryInterval = retryInterval,
      updateStartProvider = updateStartProvider,
      sourceQueueFactory = SourceQueueFactory.apply(spawner, bufferSize = 8, overflowStrategy = OverflowStrategy.fail),
    )

  private def runWithProbe(initialSegment: CandleHistorySegment): TestSubscriber.Probe[CandleHistorySegment] =
    stream().source(initialSegment).runWith(TestSink.probe[CandleHistorySegment])

  implicit val system: ActorSystem[Nothing] = actorSystem

  test("just obtaining a source does not trigger any request") {
    stream().source(segment(sec(10), secs(5))())
    candleLoader.expectNoCall()
    spawner.expectNoSpawn()
  }

  test("when running a source it immediately requests the candles from the update start") {
    updateStartProvider = _.start
    stream().source(segment(sec(10), secs(5))()).runWith(Sink.ignore)
    candleLoader.verify.apply(sec(10))
  }

  test("it emits an extended segment once the update is received") {
    val initialSegment = segment(sec(10), secs(5))(1, 2)
    val sinkProbe = runWithProbe(initialSegment)
    val updateSegment = segment(sec(15), secs(5))(22, 33)
    candleLoader.completeNext(updateSegment)
    sinkProbe.requestNext shouldEqual initialSegment.extendWith(updateSegment)
  }

  test("it requests the next segment for the new update start after the given interval (starting when received)") {
    updateStartProvider = _.end
    interval = 10.seconds
    val initialSegment = segment(sec(10), secs(5))(1, 2)
    val sinkProbe = runWithProbe(initialSegment)
    wait(2.seconds)
    val updateSegment = segment(sec(15), secs(5))(22, 33)
    candleLoader.completeNext(updateSegment)
    wait(9.seconds)
    candleLoader.expectNoFurtherOpenRequests()
    wait(1.seconds)
    candleLoader.verifyTimes(2).apply(*)
    candleLoader.verify.apply(sec(25))
  }

  test("it requests the candles again after an error after the given interval (starting after the error)") {
    updateStartProvider = _.start
    interval = 100.seconds
    retryInterval = 10.seconds
    val initialSegment = segment(sec(10), secs(5))(1, 2)
    val sinkProbe = runWithProbe(initialSegment)
    wait(2.seconds)
    candleLoader.failNext(ex(123))
    wait(9.seconds)
    candleLoader.expectNoFurtherOpenRequests()
    wait(1.seconds)
    candleLoader.verifyTimes(2).apply(sec(10))
  }

  test("no more requests are made when the source has been cancelled") {
    interval = 100.seconds
    retryInterval = 10.seconds
    val initialSegment = segment(sec(10), secs(5))(1, 2)
    val sinkProbe = runWithProbe(initialSegment)
    candleLoader.failNext(ex(123))
    wait(2.seconds)
    sinkProbe.cancel()
    wait(9.seconds)
    candleLoader.expectNoFurtherOpenRequests()
  }

}
