package io.liquirium.connect

import akka.actor.typed.ActorSystem
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import io.liquirium.core.helpers.CoreHelpers.ex
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.helpers.OrderHelpers.{order => o}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.FutureServiceMock
import io.liquirium.core.{Market, Order}
import io.liquirium.helpers.TypedActorTest
import io.liquirium.util.DummyLogger
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.concurrent.Future
import scala.concurrent.duration._

class OpenOrdersStreamTest_FromOpenOrdersProvider extends TypedActorTest with TestWithMocks {

  implicit val system: ActorSystem[Nothing] = actorSystem

  val orderProvider = new FutureServiceMock[Option[Market] => Future[Set[Order]], Set[Order]](_.apply(*))
  var optMarket: Option[Market] = None
  var interval: FiniteDuration = 1.second
  var retryDelay: FiniteDuration = 1.second

  private def run(): TestSubscriber.Probe[OpenOrdersStream.Update] = {
    val sinkProbe: TestSubscriber.Probe[OpenOrdersStream.Update] = OpenOrdersStream
      .fromOrdersProvider(optMarket, interval, retryDelay, orderProvider.instance, DummyLogger)
      .runWith(TestSink.probe[OpenOrdersStream.Update])
    sinkProbe.request(100)
    sinkProbe
  }

  test("it requests the orders from the order provider with the given market") {
    optMarket = Some(m(123))
    run()
    orderProvider.verify.apply(Some(m(123)))
  }

  test("it emits an update with the orders when the orders are received (no cancel quantities)") {
    val sinkProbe = run()
    orderProvider.completeNext(Set(o(1), o(2)))
    sinkProbe.requestNext() shouldEqual OpenOrdersStream.Update(Set(o(1), o(2)))
  }

  test("after receiving the orders it requests them again only after the interval") {
    interval = 10.seconds
    run()
    wait(2.seconds)
    orderProvider.completeNext(Set())
    wait(9.seconds)
    orderProvider.verifyTimes(1).apply(*)
    wait(1.second)
    orderProvider.verifyTimes(2).apply(*)
  }

  test("a retry after a failure takes place after the retry delay") {
    interval = 10.seconds
    retryDelay = 3.seconds
    run()
    orderProvider.failNext(ex(1))
    wait(2.seconds)
    orderProvider.verifyTimes(1).apply(*)
    wait(1.second)
    orderProvider.verifyTimes(2).apply(*)
  }

  test("no more requests are made when the source has been cancelled") {
    interval = 10.seconds
    val sinkProbe = run()
    orderProvider.completeNext(Set())
    wait(2.seconds)
    sinkProbe.cancel()
    wait(9.seconds)
    orderProvider.expectNoFurtherOpenRequests()
  }

}
