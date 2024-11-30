package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput.{CompletedOperationRequest, CompletedOperationRequestsInSession, OrderSnapshotHistoryInput, SimulatedOpenOrdersInput, TradeHistoryInput}
import io.liquirium.bot.simulation.helpers.FakeCandleSimulator
import io.liquirium.core._
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.TradeHelpers.tradeHistorySegment
import io.liquirium.core.helpers.{CandleHelpers, MarketHelpers, TestWithMocks}
import io.liquirium.core.orderTracking.OpenOrdersSnapshot
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{openOrdersHistory, openOrdersSnapshot}
import io.liquirium.eval.helpers.SimpleFakeContext
import io.liquirium.eval.{Eval, IncrementalSeq, Input, InputEval, InputRequest, InputUpdate, UpdatableContext, Value}
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant

class CandleSimulatorMarketplaceTest extends TestWithMocks {

  protected var currentContext: UpdatableContext = SimpleFakeContext(Map())
  protected var currentMarketplace: CandleSimulatorMarketplace = _
  protected val defaultMarket: Market = MarketHelpers.market(123)
  protected val candlesEval: Eval[CandleHistorySegment] = InputEval(mock(classOf[Input[CandleHistorySegment]]))
  protected var candleSimulator: FakeCandleSimulator = FakeCandleSimulator()
  protected var orderIds: Stream[String] = Stream.from(1).map(_.toString).take(10)
  protected var simulationStartTime: Instant = sec(0)
  protected var lastInputUpdate: InputUpdate = _


  def c(n: Int): Candle = CandleHelpers.candle(start = sec(n), length = secs(1))

  protected def completeTradeHistoryInput: TradeHistoryInput = TradeHistoryInput(defaultMarket, simulationStartTime)
  protected def orderHistoryInput: OrderSnapshotHistoryInput = OrderSnapshotHistoryInput(defaultMarket)
  protected def simulatedOpenOrdersInput: SimulatedOpenOrdersInput = SimulatedOpenOrdersInput(defaultMarket)

  protected def makeInitialMarketplace(): CandleSimulatorMarketplace =
    CandleSimulatorMarketplace(
      market = defaultMarket,
      candlesEval = candlesEval,
      simulator = candleSimulator,
      orderIds = orderIds,
      simulationStartTime = simulationStartTime,
    )

  def updateContext(update: SimpleFakeContext => SimpleFakeContext): Unit = {
    currentContext = update(currentContext.asInstanceOf[SimpleFakeContext])
  }

  def fakeCandles(cc: Candle*): Unit = {
      updateContext(_.fake(candlesEval, CandleHelpers.candleHistorySegment(cc.head, cc.tail: _*)))
  }

  def fakeEmptyCandleHistory(start: Instant): Unit = {
    updateContext(_.fake(candlesEval, CandleHelpers.candleHistorySegment(start, secs(1))()))
  }

  def fakeMissingInputsForCandles(inputRequest: InputRequest): Unit = {
    updateContext(_.fakeMissingInputs(candlesEval, inputRequest))
  }

  def fakeTradeHistory(tt: Trade*): Unit = {
    val segment = TradeHistorySegment.fromForwardTrades(simulationStartTime, tt)
    updateContext(_.fake(completeTradeHistoryInput, segment))
  }

  def fakeMissingTradeHistory(): Unit = {
    updateContext(_.fakeMissing(completeTradeHistoryInput))
  }

  def fakeOrderHistory(oo: OpenOrdersSnapshot*): Unit = {
    updateContext(_.fake(
      OrderSnapshotHistoryInput(defaultMarket),
      openOrdersHistory(oo: _*),
    ))
  }

  def fakeMissingOrderHistory(): Unit = {
    updateContext(_.fakeMissing(orderHistoryInput))
  }

  def fakeEmptyOrderHistory(): Unit = {
    fakeOrderHistory(openOrdersSnapshot(Set[Order](), sec(0)))
  }

  def fakeCompletedOperationRequests(ctr: CompletedOperationRequest*): Unit = {
    updateContext(_.fake(CompletedOperationRequestsInSession, IncrementalSeq.from(ctr)))
  }

  def fakeMissingCompletedOperationRequests(): Unit = {
    updateContext(_.fakeMissing(CompletedOperationRequestsInSession))
  }

  def assertCompleteTradeHistory(tt: Trade*): Unit = {
    currentContext.evaluate(InputEval(completeTradeHistoryInput))._1 shouldEqual
      Value(tradeHistorySegment(sec(0))(tt: _*))
  }

  def assertOrderHistory(snapshots: OpenOrdersSnapshot*): Unit = {
    currentContext.evaluate(InputEval(orderHistoryInput))._1 shouldEqual
      Value(openOrdersHistory(snapshots: _*))
  }

  def assertSimulatedOpenOrders(orders: Set[Order]): Unit = {
    currentContext.evaluate(InputEval(simulatedOpenOrdersInput))._1 shouldEqual Value(orders)
  }

  def assertCompletedTradeRequests(ctr: CompletedOperationRequest*): Unit = {
    currentContext.evaluate(InputEval(CompletedOperationRequestsInSession))._1 shouldEqual
      Value(IncrementalSeq.from(ctr))
  }

  def assertOpenOrderIds(ids: String*): Unit = {
    currentContext.evaluate(InputEval(orderHistoryInput))._1
      .get.lastSnapshot.orderIds shouldEqual ids.toSet
  }

}
