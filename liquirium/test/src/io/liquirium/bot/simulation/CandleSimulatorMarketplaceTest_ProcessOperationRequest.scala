package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput.{CompletedOperationRequest, CompletedOperationRequestsInSession, OrderSnapshotHistoryInput}
import io.liquirium.bot.OperationRequestMessage
import io.liquirium.bot.helpers.OperationRequestHelpers
import io.liquirium.bot.helpers.OperationRequestHelpers.completedOperationRequest
import io.liquirium.core.TradeRequestResponseMessage.NoSuchOpenOrderCancelFailure
import io.liquirium.core._
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.openOrdersSnapshot
import io.liquirium.eval.InputRequest
import io.liquirium.eval.helpers.EvalHelpers.inputRequest
import io.liquirium.util.AbsoluteQuantity
import org.scalatest.matchers.must.Matchers.not
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper, thrownBy}


class CandleSimulatorMarketplaceTest_ProcessOperationRequest extends CandleSimulatorMarketplaceTest {

  private def orderRequest(n: Int, market: Market = defaultMarket) = OperationRequestHelpers.orderRequest(market, n)

  private def cancelRequest(s: String, market: Market = defaultMarket) =
    OperationRequestHelpers.cancelRequest(s, market)

  def msg(n: Int, tradeRequest: OperationRequest): OperationRequestMessage =
    OperationRequestMessage(OperationRequestHelpers.id(n), tradeRequest)

  private def process(trm: OperationRequestMessage): Unit = {
    if (currentMarketplace == null) {
      currentMarketplace = makeInitialMarketplace()
    }
    val (inputUpdate, newMarketplace) = currentMarketplace.processOperationRequest(trm, currentContext).right.get
    currentContext = currentContext.update(inputUpdate)
    currentMarketplace = newMarketplace
  }

  private def expectInputRequestUponProcess(trm: OperationRequestMessage)(ir: InputRequest): Unit = {
    if (currentMarketplace == null) {
      currentMarketplace = makeInitialMarketplace()
    }
    currentMarketplace.processOperationRequest(trm, currentContext) shouldEqual Left(ir)
  }

  test("upon an order request the simulated orders and order history are updated and time is taken from the candles") {
    orderIds = List("A", "B").toStream
    simulationStartTime = sec(100)
    fakeCandles(c(100), c(101))
    fakeOrderHistory(
      openOrdersSnapshot(sec(100), order(42)),
    )
    fakeCompletedOperationRequests()
    val expectedNewOrder = orderRequest(123).toExactOrder("A").copy()
    process(msg(1, orderRequest(123)))
    assertOrderHistory(
      openOrdersSnapshot(sec(100), order(42)),
      openOrdersSnapshot(sec(102), order(42), expectedNewOrder)
    )
    assertSimulatedOpenOrders(Set(order(42), expectedNewOrder))
  }

  test("the internal time is advanced according to the candles when an order request is handled") {
    orderIds = List("A", "B").toStream
    simulationStartTime = sec(0)
    fakeCandles(c(100), c(101))
    fakeOrderHistory(
      openOrdersSnapshot(sec(100), order(42)),
    )
    fakeCompletedOperationRequests()
    process(msg(1, orderRequest(123)))
    currentMarketplace.lastCandleEndTime shouldEqual sec(102)
  }

  test("it yields an input request if the order history is missing") {
    orderIds = List("A", "B").toStream
    fakeCandles(c(100), c(101))
    fakeMissingOrderHistory()
    expectInputRequestUponProcess(msg(1, orderRequest(123)))(
      inputRequest(OrderSnapshotHistoryInput(defaultMarket))
    )
  }

  test("the completed requests are extended with another completed order request with the order id") {
    orderIds = List("A", "B").toStream
    simulationStartTime = sec(100)
    fakeCandles(c(100), c(101))
    fakeCompletedOperationRequests(completedOperationRequest(1))
    fakeEmptyOrderHistory()
    val expectedOrder = orderRequest(123).toExactOrder("A")
    process(msg(1, orderRequest(123)))
    val newCompletedRequest =
      CompletedOperationRequest(
        completionTime = sec(102),
        requestMessage = msg(1, orderRequest(123)),
        response = Right(OrderRequestConfirmation(Some(expectedOrder), Seq())),
      )
    assertCompletedTradeRequests(completedOperationRequest(1), newCompletedRequest)
  }

  test("it yields an input request when the completed operation requests are missing") {
    orderIds = List("A", "B").toStream
    fakeCandles(c(100), c(101))
    fakeEmptyOrderHistory()
    fakeMissingCompletedOperationRequests()
    expectInputRequestUponProcess(msg(1, orderRequest(123)))(inputRequest(CompletedOperationRequestsInSession))
  }

  test("a cancel request removes the respective order from history and simulated orders if present") {
    fakeCandles(c(100), c(101))
    fakeOrderHistory(
      openOrdersSnapshot(sec(100), order(42), order(43)),
    )
    fakeCompletedOperationRequests()
    process(msg(1, cancelRequest(order(42).id)))
    assertOrderHistory(
      openOrdersSnapshot(sec(100), order(42), order(43)),
      openOrdersSnapshot(sec(102), order(43)),
    )
    assertSimulatedOpenOrders(Set(order(43)))
  }

  test("upon cancel the completed requests are extended with a cancel request with the correct rest quantity") {
    fakeCandles(c(100), c(101))
    fakeCompletedOperationRequests(completedOperationRequest(1))
    val o = order(42).copy(
      fullQuantity = dec(-7),
      openQuantity = dec(-5),
    )
    fakeOrderHistory(
      openOrdersSnapshot(sec(100), o, order(43)),
    )
    process(msg(1, cancelRequest(order(42).id)))
    val confirmation = CancelRequestConfirmation(Some(AbsoluteQuantity(dec(5))))
    val newCompletedRequest = CompletedOperationRequest(
      completionTime = sec(102),
      requestMessage = msg(1, cancelRequest(order(42).id)),
      response = Right(confirmation),
    )
    assertCompletedTradeRequests(completedOperationRequest(1), newCompletedRequest)
  }

  test("the internal time is advanced according to the candles when a cancel happens") {
    simulationStartTime = sec(0)
    fakeCandles(c(100), c(101))
    fakeCompletedOperationRequests(completedOperationRequest(1))
    fakeOrderHistory(
      openOrdersSnapshot(sec(100), order(42)),
    )
    process(msg(1, cancelRequest(order(42).id)))
    currentMarketplace.lastCandleEndTime shouldEqual sec(102)
  }

  test("the internal time is advanced according to the candles when a cancel fails") {
    simulationStartTime = sec(0)
    fakeCandles(c(100), c(101))
    fakeCompletedOperationRequests(completedOperationRequest(1))
    fakeOrderHistory(
      openOrdersSnapshot(sec(100), order(42)),
    )
    process(msg(1, cancelRequest(order(43).id)))
    currentMarketplace.lastCandleEndTime shouldEqual sec(102)
  }

  test("a cancel failure is returned when there is no such order") {
    fakeCandles(c(100), c(101))
    fakeCompletedOperationRequests(completedOperationRequest(1))
    fakeOrderHistory(
      openOrdersSnapshot(sec(100), order(43)),
    )
    process(msg(1, cancelRequest(order(42).id)))
    val failure = NoSuchOpenOrderCancelFailure(order(42).id)
    val newCompletedRequest = CompletedOperationRequest(
      completionTime = sec(102),
      requestMessage = msg(1, cancelRequest(order(42).id)),
      response = Left(failure),
    )
    assertCompletedTradeRequests(completedOperationRequest(1), newCompletedRequest)
  }

  test("the order id stream is only advanced when a new order has been generated") {
    fakeCandles(c(4), c(5))
    fakeEmptyOrderHistory()
    fakeCompletedOperationRequests()
    orderIds = List("A", "B", "C").toStream
    process(msg(1, orderRequest(123)))
    process(msg(2, cancelRequest("333")))
    process(msg(3, orderRequest(234)))
    assertOpenOrderIds("A", "B")
  }

  test("an exception is thrown when an order request market doesn't match") {
    // currently required so we don't get an exception for missing candles
    // can maybe be removed in the future
    fakeCandles(c(4), c(5))
    m(2) should not equal defaultMarket // just make sure we do in fact test with a different market
    an[Exception] shouldBe thrownBy(process(msg(1, orderRequest(1, m(2)))))
    an[Exception] shouldBe thrownBy(process(msg(2, cancelRequest("1", m(2)))))
  }

  test("an input request is returned when the candles eval is not available") {
    fakeMissingInputsForCandles(inputRequest(123))
    expectInputRequestUponProcess(msg(1, orderRequest(123)))(inputRequest(123))
  }

}
