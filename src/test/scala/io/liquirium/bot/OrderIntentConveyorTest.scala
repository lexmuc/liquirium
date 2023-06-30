package io.liquirium.bot

import io.liquirium.bot.helpers.OperationRequestHelpers.{operationRequestMessage, operationRequestId => opId}
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.Order.BasicOrderData
import io.liquirium.core.helpers.OperationIntentHelpers.{cancelIntent, orderIntent}
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.helpers.{MarketHelpers, TestWithMocks}
import io.liquirium.core._
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.eval.helpers.EvalTest
import io.liquirium.util.NumberPrecision


class OrderIntentConveyorTest extends EvalTest with TestWithMocks {

  private val market: Market = MarketHelpers.market(123)
  private val orderConstraintsEval = fakeEvalWithValue(
    OrderConstraints(
      pricePrecision = NumberPrecision.Infinite,
      quantityPrecision = NumberPrecision.Infinite,
    )
  )

  private val isInSyncEval = testEval[Boolean]()
  private val hasOpenRequestsEval = testEval[Boolean]()
  private val orderSyncerEval = testEval[OrderIntentSyncer]()
  private val openOrdersEval = testEval[Set[Order]]()
  private val nextMessageIdsEval = testEval[Stream[OperationRequestId]]()

  private var orderIntents: Seq[OrderIntent] = Seq()

  def fakeOrderIntents(intents: OrderIntent*): Unit = {
    orderIntents = intents
  }

  def fakeInSync(inSync: Boolean): Unit = {
    fakeEvalValue(isInSyncEval, inSync)
  }

  def fakeOpenRequests(hasOpenRequests: Boolean): Unit = {
    fakeEvalValue(hasOpenRequestsEval, hasOpenRequests)
  }

  def fakeOrderConstraints(
    pricePrecision: NumberPrecision,
    orderQuantityPrecision: NumberPrecision,
  ): Unit = {
    fakeEvalValue(orderConstraintsEval, OrderConstraints(
      pricePrecision = pricePrecision,
      quantityPrecision = orderQuantityPrecision,
    ))
  }

  def fakeSyncer(
    openOrders: Set[BasicOrderData],
    orderIntents: Seq[OrderIntent],
  )(operationIntents: OperationIntent*): Unit = {
    val syncer = mock[OrderIntentSyncer]
    syncer.apply(orderIntents, openOrders) returns operationIntents
    fakeEvalValue(orderSyncerEval, syncer)
  }

  def fakeOpenOrders(oo: Order*): Unit = {
    fakeEvalValue(openOrdersEval, oo.toSet)
  }

  def fakeNextMessageIds(ids: OperationRequestId*): Unit = {
    fakeEvalValue(nextMessageIdsEval, ids.toStream)
  }

  def assertOutput(oo: BotOutput*): Unit = {
    evaluate(conveyorEval).get.apply(orderIntents) shouldEqual oo
  }

  private def conveyorEval = OrderIntentConveyor(
    market = market,
    orderConstraintsEval = orderConstraintsEval,
    orderIntentSyncer = orderSyncerEval,
    openOrdersEval = openOrdersEval,
    isInSyncEval = isInSyncEval,
    hasOpenRequestsEval = hasOpenRequestsEval,
    nextMessageIdsEval = nextMessageIdsEval,
  )

  test("order intents and open orders are sent to the syncer and when its output is empty so is the conveyor output") {
    fakeInSync(true)
    fakeOpenRequests(false)
    fakeNextMessageIds()
    fakeOpenOrders(order(1))
    fakeOrderIntents(orderIntent(1))
    fakeSyncer(Set(order(1)), Seq(orderIntent(1)))()
    assertOutput()
  }

  test("when there is output, messages are numbered and intents are converted to requests without modifiers") {
    fakeInSync(true)
    fakeOpenRequests(false)
    fakeNextMessageIds(opId(1), opId(2), opId(3))
    fakeOpenOrders(order(1))
    fakeOrderIntents(orderIntent(1))
    fakeSyncer(Set(order(1)), Seq(orderIntent(1)))(
      orderIntent(1),
      cancelIntent("O2"),
    )
    assertOutput(
      operationRequestMessage(opId(1), orderIntent(1).toOperationRequest(market, Set())),
      operationRequestMessage(opId(2), cancelIntent("O2").toOperationRequest(market)),
    )
  }

  test("order intent price and quantity are adapted to match the order constraints") {
    fakeInSync(true)
    fakeOpenRequests(false)
    fakeNextMessageIds(opId(1), opId(2), opId(3))
    fakeOrderIntents(
      orderIntent("1.001", at = "2.09"),
      orderIntent("-1.001", at = "4.01"),
    )
    fakeOrderConstraints(
      pricePrecision = NumberPrecision.multipleOf(dec("0.1")),
      orderQuantityPrecision = NumberPrecision.multipleOf(dec("1.0")),
    )
    val adjustedOrderIntent1 = orderIntent("1.00", at = "2.0")
    val adjustedOrderIntent2 = orderIntent("-1.00", at = "4.1")
    fakeOpenOrders(order(1))
    fakeSyncer(Set(order(1)), Seq(adjustedOrderIntent1, adjustedOrderIntent2))(
      orderIntent(1),
    )
    assertOutput(
      operationRequestMessage(opId(1), orderIntent(1).toOperationRequest(market, Set())),
    )
  }

  test("order intents are ignored when they cannot be adapted") {
    fakeInSync(true)
    fakeOpenRequests(false)
    fakeNextMessageIds(opId(1), opId(2), opId(3))
    fakeOrderIntents(
      orderIntent("0.01", at = "2.09"),
      orderIntent("1", at = "4"),
    )
    fakeOrderConstraints(
      pricePrecision = NumberPrecision.multipleOf(dec("0.1")),
      orderQuantityPrecision = NumberPrecision.multipleOf(dec("1.0")),
    )
    fakeOpenOrders(order(1))
    fakeSyncer(Set(order(1)), Seq(orderIntent("1", at = "4")))(
      orderIntent(1),
    )
    assertOutput(
      operationRequestMessage(opId(1), orderIntent(1).toOperationRequest(market, Set())),
    )
  }

  test("no output is sent when we are not in sync") {
    fakeInSync(false)
    fakeOpenRequests(false)
    fakeNextMessageIds(opId(1), opId(2), opId(3))
    fakeOpenOrders(order(1))
    fakeOrderIntents(orderIntent(1))
    fakeSyncer(Set(order(1)), Seq(orderIntent(1)))(
      orderIntent(1),
      cancelIntent("O2"),
    )
    assertOutput()
  }

  test("no output is sent when there are open requests") {
    fakeInSync(true)
    fakeOpenRequests(true)
    fakeNextMessageIds(opId(1), opId(2), opId(3))
    fakeOpenOrders(order(1))
    fakeOrderIntents(orderIntent(1))
    fakeSyncer(Set(order(1)), Seq(orderIntent(1)))(
      orderIntent(1),
      cancelIntent("O2"),
    )
    assertOutput()
  }

}