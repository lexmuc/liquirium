package io.liquirium.bot.simulation

import io.liquirium.bot.BotInput.{OrderSnapshotHistoryInput, TradeHistoryInput}
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.FakeOrderSet
import io.liquirium.core.helpers.OrderHelpers.{order => o}
import io.liquirium.core.helpers.TradeHelpers.{trade => t}
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.openOrdersSnapshot
import io.liquirium.core.{Candle, OrderSet, Trade}
import io.liquirium.eval.Input
import io.liquirium.eval.helpers.EvalHelpers.{input, inputRequest}
import io.liquirium.eval.helpers.SimpleFakeContext

class CandleSimulatorMarketplaceTest_ProcessPriceUpdate extends CandleSimulatorMarketplaceTest {

  def orders(n: Int): FakeOrderSet = FakeOrderSet(Set(o(n), o(n + 1)))

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

  def fakeSimulatorOutput(orders: OrderSet)(trades: Trade*): Unit = {
    candleSimulator = candleSimulator.addOutput(trades, orders)
  }

  def expectSimulatorInput(orders: OrderSet, candle: Candle): Unit = {
    candleSimulator = candleSimulator.addExpectedInput(orders, candle)
  }

  test("it returns an input request when the candle input is missing") {
    fakeOrderHistory(openOrdersSnapshot(orders(0), sec(0)))
    fakeTradeHistory(t(1))
    fakeMissingInputsForCandles(inputRequest(input(123)))
    lastKnownCandleEndTime = sec(5)
    expectInputRequest(input(123))
  }

  test("trades resulting from the simulation of all new candles are appended to the history") {
    fakeOrderHistory(openOrdersSnapshot(orders(0), sec(0)))
    fakeCandles(c(4), c(5), c(6))
    fakeTradeHistory(t(1))
    fakeSimulatorOutput(orders(1))(t(2), t(3))
    fakeSimulatorOutput(orders(2))(t(4))
    lastKnownCandleEndTime = sec(5)
    processPriceUpdate()
    assertCompleteTradeHistory(t(1), t(2), t(3), t(4))
  }

  test("it returns an input request when the tade history is missing") {
    fakeOrderHistory(openOrdersSnapshot(orders(0), sec(0)))
    fakeCandles(c(4), c(5))
    fakeMissingTradeHistory()
    fakeSimulatorOutput(orders(1))(t(1), t(2))
    lastKnownCandleEndTime = sec(5)
    expectInputRequest(TradeHistoryInput(defaultMarket, sec(0)))
  }

  test("the order history is extended with all new order sets and the proper times") {
    fakeTradeHistory()
    fakeOrderHistory(
      openOrdersSnapshot(orders(1), sec(5)),
    )
    fakeTradeHistory()
    fakeCandles(c(4), c(5), c(6))
    fakeSimulatorOutput(orders(2))(t(2))
    fakeSimulatorOutput(orders(3))(t(3))
    lastKnownCandleEndTime = sec(5)
    processPriceUpdate()
    assertOrderHistory(
      openOrdersSnapshot(orders(1), sec(5)),
      openOrdersSnapshot(orders(2), sec(6)),
      openOrdersSnapshot(orders(3), sec(7)),
    )
  }

  test("it returns an input request when the order history is missing") {
    fakeMissingOrderHistory()
    fakeTradeHistory()
    fakeCandles(c(4), c(5))
    fakeSimulatorOutput(orders(2))(t(2))
    lastKnownCandleEndTime = sec(5)
    expectInputRequest(OrderSnapshotHistoryInput(defaultMarket))
  }

  test("the respectively last known order set is fed to the candle simulator with the current candle") {
    fakeOrderHistory(
      openOrdersSnapshot(orders(1), sec(5)),
    )
    fakeCandles(c(4), c(5), c(6))
    fakeTradeHistory()
    expectSimulatorInput(orders(1), c(5))
    fakeSimulatorOutput(orders(2))(t(2))
    expectSimulatorInput(orders(2), c(6))
    fakeSimulatorOutput(orders(3))(t(3))
    lastKnownCandleEndTime = sec(5)
    processPriceUpdate()
  }

  test("nothing happens when no candles are obtained at all") {
    fakeOrderHistory(openOrdersSnapshot(OrderSet.empty, sec(0)))
    fakeEmptyCandleHistory(sec(0))
    fakeTradeHistory(t(1))
    processPriceUpdate()
    assertCompleteTradeHistory(t(1))
    currentMarketplace shouldEqual makeInitialMarketplace()
  }

  test("nothing happens when no new candles are obtained") {
    fakeCandles(c(1))
    fakeOrderHistory(
      openOrdersSnapshot(orders(1), sec(1)),
    )
    fakeTradeHistory(t(1))
    lastKnownCandleEndTime = sec(3)
    processPriceUpdate()
    assertCompleteTradeHistory(t(1))
    currentMarketplace shouldEqual makeInitialMarketplace()
    assertCompleteTradeHistory(t(1))
    assertOrderHistory(
      openOrdersSnapshot(orders(1), sec(1)),
    )
  }

  test("it keeps track of which candles have already been processed") {
    fakeCandles(c(4), c(5))
    fakeOrderHistory(
      openOrdersSnapshot(orders(1), sec(5)),
    )
    fakeTradeHistory()
    expectSimulatorInput(orders(1), c(5))
    fakeSimulatorOutput(orders(2))(t(2))
    expectSimulatorInput(orders(2), c(6))
    lastKnownCandleEndTime = sec(5)
    processPriceUpdate()
    fakeCandles(c(4), c(5), c(6))
    processPriceUpdate()
  }

}
