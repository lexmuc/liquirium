package io.liquirium.bot

import akka.actor.typed.ActorSystem
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import io.liquirium.bot.BotInput.{CompletedOperationRequest, CompletedOperationRequestsInSession, TimeInput}
import io.liquirium.bot.OperationRequestProcessor.UnavailableExchangeException
import io.liquirium.bot.helpers.OperationRequestHelpers
import io.liquirium.bot.helpers.OperationRequestHelpers._
import io.liquirium.connect.ExchangeConnector
import io.liquirium.core.helpers.CoreHelpers.{ex, sec, secs}
import io.liquirium.core.helpers.MarketHelpers.{exchangeId, market}
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.core.helpers.async.{AsyncTestWithControlledTime, FutureServiceMock}
import io.liquirium.core.{ExchangeId, OperationRequest, OperationRequestSuccessResponse}
import io.liquirium.eval.IncrementalSeq
import io.liquirium.helpers.FakeClock
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.concurrent.Future

class OperationRequestProcessorTest extends AsyncTestWithControlledTime with TestWithMocks {

  private implicit val system: ActorSystem[Nothing] = actorSystem

  private val clock = FakeClock(sec(0))

  private val exchangeA = exchangeId("A")
  private val connectorA = mock(classOf[ExchangeConnector])
  private val submitRequestMockA =
    new FutureServiceMock[ExchangeConnector, OperationRequestSuccessResponse[_]](_.submitRequest(*), Some(connectorA))

  private val exchangeB = exchangeId("B")
  private val connectorB = mock(classOf[ExchangeConnector])
  private val submitRequestMockB =
    new FutureServiceMock[ExchangeConnector, OperationRequestSuccessResponse[_]](_.submitRequest(*), Some(connectorB))

  private val unknownExchange = exchangeId("X")

  type HistoryType = IncrementalSeq[CompletedOperationRequest]

  def msg(n: Int, tradeRequest: OperationRequest): OperationRequestMessage =
    OperationRequestMessage(OperationRequestHelpers.id(n), tradeRequest)

  private val connectorsByExchangeId = mock(classOf[ExchangeId => Future[ExchangeConnector]])
  connectorsByExchangeId.apply(exchangeA) returns Future{ connectorA }
  connectorsByExchangeId.apply(exchangeB) returns Future { connectorB }

  val processor = new OperationRequestProcessor(connectorsByExchangeId, clock, spawner)

  def subscribe(): TestSubscriber.Probe[HistoryType] = {
    val optSource = processor.getInputUpdateStream(CompletedOperationRequestsInSession)
    optSource.get.runWith(TestSink.probe[HistoryType])
  }

  test("completed trade requests in session can be subscribed to and an empty history is immediately output") {
    val sinkProbe = subscribe()
    sinkProbe.requestNext() shouldEqual IncrementalSeq.empty[CompletedOperationRequest]
  }

  test("requests for other inputs return None") {
    processor.getInputUpdateStream(TimeInput(secs(10))) shouldEqual None
  }

  test("sending an operation request yields true as response, other trader outputs yield false") {
    processor.processOutput(operationRequestMessage(id(1), operationRequest(market(exchangeA, 1), 1))) shouldBe true
    processor.processOutput(SimpleBotLogEntry("log something")) shouldBe false
  }

  test("an operation request is forwarded to the correct exchange") {
    processor.processOutput(operationRequestMessage(id(333), operationRequest(market(exchangeA, 1), 1)))
    submitRequestMockA.verify.submitRequest(operationRequest(market(exchangeA, 1), 1))
    processor.processOutput(operationRequestMessage(id(444), operationRequest(market(exchangeA, 2), 2)))
    submitRequestMockA.verify.submitRequest(operationRequest(market(exchangeA, 2), 2))
    processor.processOutput(operationRequestMessage(id(555), operationRequest(market(exchangeB, 1), 3)))
    submitRequestMockB.verify.submitRequest(operationRequest(market(exchangeB, 1), 3))
  }

  test("as soon as a request completes (success or failure) the request history is updated with the correct time") {
    val m = market(exchangeA, 1)
    val msg1 = msg(1, operationRequest(m, 1))
    val msg2 = msg(2, operationRequest(m, 2))
    val sinkProbe = subscribe()
    sinkProbe.requestNext() shouldEqual IncrementalSeq.empty[CompletedOperationRequest]
    processor.processOutput(msg1)
    processor.processOutput(msg2)

    clock.set(sec(123))
    submitRequestMockA.completeNext(successResponse(msg1, 123))
    sinkProbe.requestNext() shouldEqual IncrementalSeq.from(Seq(
      OperationRequestHelpers.successfulRequestWithTime(sec(123), msg1, successResponse(msg1, 123)),
    ))

    clock.set(sec(234))
    submitRequestMockA.failNext(ex(777))
    sinkProbe.requestNext() shouldEqual IncrementalSeq.from(Seq(
      successfulRequestWithTime(sec(123), msg1, successResponse(msg1, 123)),
      failedRequestWithTime(sec(234), msg2, ex(777)),
    ))
  }

  test("when subscribing to the request history after the first request the complete history is provided as well") {
    val m = market(exchangeA, 1)
    val r1 = operationRequest(m, 1)
    val requestMessage = msg(1, r1)
    processor.processOutput(operationRequestMessage(id(1), r1))
    clock.set(sec(123))
    submitRequestMockA.completeNext(successResponse(requestMessage, 123))

    val sinkProbe = subscribe()
    sinkProbe.requestNext() shouldEqual IncrementalSeq.from(Seq(
      successfulRequestWithTime(sec(123), requestMessage, successResponse(requestMessage, 123)),
    ))
  }

  test("a request fails immediately when the exchange is unknown") {
    val m = market(unknownExchange, 1)
    connectorsByExchangeId.apply(unknownExchange) returns Future.failed(ex(123))
    val r1 = operationRequest(m, 1)
    val requestMessage = msg(1, r1)
    clock.set(sec(123))
    processor.processOutput(operationRequestMessage(id(1), r1))

    val sinkProbe = subscribe()
    sinkProbe.requestNext() shouldEqual IncrementalSeq.from(Seq(
      failedRequestWithTime(sec(123), requestMessage, UnavailableExchangeException(unknownExchange, ex(123))),
    ))
  }

}
