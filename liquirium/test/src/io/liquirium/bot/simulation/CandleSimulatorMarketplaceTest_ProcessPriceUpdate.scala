package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput.{OrderSnapshotHistoryInput, TradeHistoryInput}
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.TradeHelpers
import io.liquirium.core.helpers.OrderHelpers.{order => o}
import io.liquirium.core.helpers.TradeHelpers.{trade => t}
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.openOrdersSnapshot
import io.liquirium.core.{Candle, Order, Trade}
import io.liquirium.eval.Input
import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest}
import io.liquirium.eval.helpers.SimpleFakeContext
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CandleSimulatorMarketplaceTest_ProcessPriceUpdate extends CandleSimulatorMarketplaceTest {

  def orders(n: Int): Set[Order] = Set(o(n), o(n + 1))

  def processPriceUpdate(): Unit = {
    if (currentMarketplace == null) {
      currentMarketplace = makeInitialMarketplace()
    }
    val (newContext, newMarketplace) = currentMarketplace.processPriceUpdates(currentContext).right.get
    currentContext = newContext.asInstanceOf[SimpleFakeContext]
    currentMarketplace = newMarketplace
  }

  def expectInputRequest(inputs: Input[_]*): Unit = {
    if (currentMarketplace == null) {
      currentMarketplace = makeInitialMarketplace()
    }
    currentMarketplace.processPriceUpdates(currentContext) shouldEqual Left(inputRequest(inputs: _*))
  }

  def fakeSimulatorOutput(orders: Set[Order])(trades: Trade*): Unit = {
    candleSimulator = candleSimulator.addOutput(trades, orders)
  }

  def expectSimulatorInput(orders: Set[Order], candle: Candle): Unit = {
    candleSimulator = candleSimulator.addExpectedInput(orders, candle)
  }

  test("it returns an input request when the candle input is missing") {
    simulationStartTime = sec(0)
    fakeOrderHistory(openOrdersSnapshot(orders(0), sec(0)))
    fakeTradeHistory(t(7))
    fakeMissingInputsForCandles(inputRequest(input(123)))
    expectInputRequest(input(123))
  }

  test("trades resulting from the simulation of all new candles are appended to the history") {
    def t(n: Int) = TradeHelpers.trade(sec(n), n.toString)
    simulationStartTime = sec(5)
    fakeOrderHistory(openOrdersSnapshot(orders(0), sec(0)))
    fakeCandles(c(4), c(5), c(6))
    fakeTradeHistory(t(6))
    fakeSimulatorOutput(orders(1))(t(7), t(8))
    fakeSimulatorOutput(orders(2))(t(9))
    processPriceUpdate()
    assertCompleteTradeHistory(t(6), t(7), t(8), t(9))
  }

  test("it returns an input request when the trade history is missing (start is simulation start)") {
    simulationStartTime = sec(5)
    fakeOrderHistory(openOrdersSnapshot(orders(0), sec(0)))
    fakeCandles(c(4), c(5))
    fakeMissingTradeHistory()
    fakeSimulatorOutput(orders(1))(t(1), t(2))
    expectInputRequest(TradeHistoryInput(defaultMarket, sec(5)))
  }

  test("the order history and simulated open orders are extended with all new order sets and the proper times") {
    def t(n: Int) = TradeHelpers.trade(sec(n), n.toString)
    simulationStartTime = sec(5)
    fakeTradeHistory()
    fakeOrderHistory(
      openOrdersSnapshot(orders(1), sec(5)),
    )
    fakeCandles(c(4), c(5), c(6))
    fakeSimulatorOutput(orders(2))(t(7))
    fakeSimulatorOutput(orders(3))(t(8))
    processPriceUpdate()
    assertOrderHistory(
      openOrdersSnapshot(orders(1), sec(5)),
      openOrdersSnapshot(orders(2), sec(6)),
      openOrdersSnapshot(orders(3), sec(7)),
    )
    assertSimulatedOpenOrders(orders(3))
  }

  test("it returns an input request when the order history is missing") {
    simulationStartTime = sec(5)
    fakeMissingOrderHistory()
    fakeTradeHistory()
    fakeCandles(c(4), c(5))
    fakeSimulatorOutput(orders(2))(t(2))
    expectInputRequest(OrderSnapshotHistoryInput(defaultMarket))
  }

  test("the respectively last known order set is fed to the candle simulator with the current candle") {
    def t(n: Int) = TradeHelpers.trade(sec(n), n.toString)
    simulationStartTime = sec(5)
    fakeOrderHistory(
      openOrdersSnapshot(orders(1), sec(5)),
    )
    fakeCandles(c(4), c(5), c(6))
    fakeTradeHistory()
    expectSimulatorInput(orders(1), c(5))
    fakeSimulatorOutput(orders(2))(t(7))
    expectSimulatorInput(orders(2), c(6))
    fakeSimulatorOutput(orders(3))(t(8))
    processPriceUpdate()
  }

  test("nothing happens when no candles are obtained at all") {
    fakeOrderHistory(openOrdersSnapshot(Set[Order](), sec(0)))
    fakeEmptyCandleHistory(sec(0))
    fakeTradeHistory(t(1))
    processPriceUpdate()
    assertCompleteTradeHistory(t(1))
    currentMarketplace shouldEqual makeInitialMarketplace()
  }

  test("nothing happens when no new candles are obtained") {
    def t(n: Int) = TradeHelpers.trade(sec(n), n.toString)
    simulationStartTime = sec(3)
    fakeCandles(c(1))
    fakeOrderHistory(
      openOrdersSnapshot(orders(1), sec(1)),
    )
    fakeTradeHistory(t(6))
    processPriceUpdate()
    assertCompleteTradeHistory(t(6))
    currentMarketplace shouldEqual makeInitialMarketplace()
    assertCompleteTradeHistory(t(6))
    assertOrderHistory(
      openOrdersSnapshot(orders(1), sec(1)),
    )
  }

  test("it keeps track of which candles have already been processed") {
    def t(n: Int) = TradeHelpers.trade(sec(n), n.toString)
    simulationStartTime = sec(5)
    fakeCandles(c(4), c(5))
    fakeOrderHistory(
      openOrdersSnapshot(orders(1), sec(5)),
    )
    fakeTradeHistory()
    expectSimulatorInput(orders(1), c(5))
    fakeSimulatorOutput(orders(2))(t(7))
    expectSimulatorInput(orders(2), c(6))
    processPriceUpdate()
    fakeCandles(c(4), c(5), c(6))
    processPriceUpdate()
  }

}
