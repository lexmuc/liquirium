package io.liquirium.bot.simulation

import io.liquirium.bot.helpers.OperationRequestHelpers
import io.liquirium.bot.helpers.OperationRequestHelpers.operationRequest
import io.liquirium.bot.{BotInput, OperationRequestMessage}
import io.liquirium.core.Market
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.eval.UpdatableContext
import io.liquirium.eval.helpers.ContextHelpers
import io.liquirium.eval.helpers.ContextHelpers.{inputUpdate, simpleFakeContext}
import io.liquirium.eval.helpers.EvalHelpers.inputRequest


class SimulationMarketplacesTest extends TestWithMocks {

  private def marketplaces(mm: SimulationMarketplace*) = SimulationMarketplaces(mm)

  private def marketplaces(factory: Market => SimulationMarketplace) = SimulationMarketplaces(factory)

  private def marketplace(market: Market, inputs: Map[BotInput[_], Any] = Map()) =
    helpers.FakeSimulationMarketplace(market, inputs)

  private def fakeContext(n: Int) = ContextHelpers.context(n)

  private def requestMessage(n: Int, market: Market): OperationRequestMessage =
    OperationRequestMessage(OperationRequestHelpers.id(n), operationRequest(market, n))

  test("an operation request message is forwarded to the respective marketplace which is then updated") {
    val r1 = requestMessage(1, m(1))
    val context = simpleFakeContext()
    val mp1 = marketplace(m(1)).addTradeRequestCompletion(r1, context, inputUpdate(123))
    val mp2 = marketplace(m(2))
    val newMp1 = mp1.processOperationRequest(r1, context).right.get._2
    newMp1 shouldNot equal(mp1)
    val (_, newState) = marketplaces(mp1, mp2).processOperationRequest(r1, context).right.get
    newState shouldEqual marketplaces(newMp1, mp2)
  }

  test("it can be created with a marketplace factory that can create marketplaces on demand") {
    val marketplaceFactory = mock[Market => SimulationMarketplace]
    val r1 = requestMessage(1, m(1))
    val context0 = simpleFakeContext()
    val context1 = context0.update(inputUpdate(1))
    val mp1 = marketplace(m(1))
      .addTradeRequestCompletion(r1, context0, inputUpdate(1))
    marketplaceFactory.apply(m(1)) returns mp1
    val (finalContext, _) = marketplaces(marketplaceFactory)
      .processOperationRequest(r1, context0).right.get
    finalContext shouldEqual context1
  }

  test("created marketplaces are stored and reused for subsequent requests") {
    val marketplaceFactory = mock[Market => SimulationMarketplace]
    val r1 = requestMessage(1, m(1))
    val r2 = requestMessage(2, m(2))
    val r3 = requestMessage(3, m(1))
    val context0 = simpleFakeContext()
    val context1 = context0.update(inputUpdate(1))
    val context2 = context1.update(inputUpdate(2))
    val context3 = context2.update(inputUpdate(3))
    val mp1 = marketplace(m(1))
      .addTradeRequestCompletion(r1, context0, inputUpdate(1))
      .addTradeRequestCompletion(r3, context2, inputUpdate(3))
    val mp2 = marketplace(m(2)).addTradeRequestCompletion(r2, context1, inputUpdate(2))
    marketplaceFactory.apply(m(1)) returns mp1
    marketplaceFactory.apply(m(2)) returns mp2
    val (finalContext, _) = marketplaces(marketplaceFactory)
      .processOperationRequest(r1, context0).right.get._2
      .processOperationRequest(r2, context1).right.get._2
      .processOperationRequest(r3, context2).right.get
    finalContext shouldEqual context3
  }

  test("after an operation request message it returns the context updated with the respective input update") {
    val r1 = requestMessage(1, m(1))
    val inputContext = mock[UpdatableContext]
    inputContext.update(inputUpdate(123)) returns fakeContext(123)
    val mp1 = marketplace(m(1)).addTradeRequestCompletion(r1, inputContext, inputUpdate(123))
    val (newContext, _) = marketplaces(mp1).processOperationRequest(r1, inputContext).right.get
    newContext shouldEqual fakeContext(123)
  }

  test("if an operation request fails with an input request it is forwarded") {
    val r1 = requestMessage(1, m(1))
    val context = simpleFakeContext()
    val mp1 = marketplace(m(1)).addTradeRequestFailure(r1, context, inputRequest(123))
    marketplaces(mp1).processOperationRequest(r1, context) shouldEqual Left(inputRequest(123))
  }

  test("in a price update the context is piped through all marketplaces and the marketplaces are updated") {
    val mp1 = marketplace(m(1)).addPriceUpdateTransition(fakeContext(1), fakeContext(2))
    val mp2 = marketplace(m(2)).addPriceUpdateTransition(fakeContext(2), fakeContext(3))
    val mp3 = marketplace(m(3)).addPriceUpdateTransition(fakeContext(3), fakeContext(4))
    val (newContext, newMarketplaces) = marketplaces(mp1, mp2, mp3).executeActivatedOrders(fakeContext(1)).right.get
    newContext shouldEqual fakeContext(4)
    newMarketplaces shouldEqual marketplaces(
      mp1.processPriceUpdates(fakeContext(1)).right.get._2,
      mp2.processPriceUpdates(fakeContext(2)).right.get._2,
      mp3.processPriceUpdates(fakeContext(3)).right.get._2,
    )
  }

  test("if a price update for one marketplace fails, it is aborted and the input request is returned") {
    val mp1 = marketplace(m(1)).addPriceUpdateTransition(fakeContext(1), fakeContext(2))
    val mp2 = marketplace(m(2)).addPriceUpdateFailure(fakeContext(2), inputRequest(123))
    val mp3 = marketplace(m(3))
    marketplaces(mp1, mp2, mp3).executeActivatedOrders(fakeContext(1)) shouldEqual Left(inputRequest(123))
  }

}
