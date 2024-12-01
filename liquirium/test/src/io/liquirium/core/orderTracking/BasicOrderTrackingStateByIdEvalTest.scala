package io.liquirium.core.orderTracking

import io.liquirium.core.{Order, Trade, TradeHistorySegment}
import io.liquirium.core.helpers.{OrderHelpers, TradeHelpers}
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.basicOrderTrackingState
import io.liquirium.eval.helpers.EvalTestWithIncrementalContext
import io.liquirium.eval.{Eval, IncrementalMap, IncrementalSeq}
import io.liquirium.util.AbsoluteQuantity
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, theSameInstanceAs}

import java.time.Instant

class BasicOrderTrackingStateByIdEvalTest
  extends EvalTestWithIncrementalContext[IncrementalMap[String, BasicOrderTrackingState]] {

  private var observationEvents: IncrementalSeq[OrderTrackingEvent.OrderObservationEvent] =
    IncrementalSeq[OrderTrackingEvent.OrderObservationEvent]()
  private var trades = TradeHistorySegment.empty(Instant.ofEpochSecond(0))
  private var operations = IncrementalSeq[OrderTrackingEvent.OperationEvent]()

  private val tradesEval = fakeInputEval[TradeHistorySegment]
  private val observationsEval = fakeInputEval[IncrementalSeq[OrderTrackingEvent.OrderObservationEvent]]
  private val operationsEval = fakeInputEval[IncrementalSeq[OrderTrackingEvent.OperationEvent]]

  override protected val evalUnderTest: Eval[IncrementalMap[String, BasicOrderTrackingState]] =
    BasicOrderTrackingStateByIdEval(
      trades = tradesEval,
      observationEvents = observationsEval,
      successfulOperations = operationsEval,
    )

//  private def fakeNewOpenOrdersSnapshot(t: Instant, orders: Order*): Unit = {
//    val snapshot = openOrdersSnapshot(OrderHelpers.orders(orders: _*), t)
//    openOrdersHistory = Some(openOrdersHistory match {
//      case None => OpenOrdersHistory.start(snapshot)
//      case Some(h) => h.appendIfChanged(snapshot)
//    })
//    updateInput(openOrdersHistoryEval, openOrdersHistory.get)
//  }

  private def fakeNewObservation(t: Instant, order: Order): Unit = {
    observationEvents = observationEvents.inc(OrderTrackingEvent.ObservationChange(t, order))
    updateInput(observationsEval, observationEvents)
  }

  private def fakeNewDisappearance(t: Instant, orderId: String): Unit = {
    observationEvents = observationEvents.inc(OrderTrackingEvent.Disappearance(t, orderId))
    updateInput(observationsEval, observationEvents)
  }

  private def fakeNewTrade(t: Trade): Unit = {
    trades = trades.inc(t)
    updateInput(tradesEval, trades)
  }

  private def fakeEmptyTrades(): Unit = {
    updateInput(tradesEval, TradeHistorySegment.empty(Instant.ofEpochSecond(0)))
  }

  private def fakeNewOperation(o: OrderTrackingEvent.OperationEvent): Unit = {
    operations = operations.inc(o)
    updateInput(operationsEval, operations)
  }

  private def fakeEmptyOperations(): Unit = {
    updateInput(operationsEval, IncrementalSeq.empty[OrderTrackingEvent.OperationEvent])
  }

  private def fakeEmptyObservationEvents(): Unit = {
    updateInput(observationsEval, IncrementalSeq.empty[OrderTrackingEvent.OrderObservationEvent])
  }

  private def t(n: Int, orderId: String) = TradeHelpers.trade(id = n.toString, orderId = Some(orderId))

  private def o(id: String, p: Int) = OrderHelpers.order(id = id, price = dec(p))

  private def oc(t: Instant, id: String, p: Int) = OrderTrackingEvent.ObservationChange(t, o(id, p))

  private def oc(t: Instant, id: String) = OrderTrackingEvent.Disappearance(t, id)

  private def te(n: Int, orderId: String) = OrderTrackingEvent.NewTrade(t(n, orderId))

  private def cancelEvent(orderId: String, n: Int) =
    OrderTrackingEvent.Cancel(sec(n), orderId, Some(AbsoluteQuantity(dec(n))))

  private def creationEvent(orderId: String, n: Int) = OrderTrackingEvent.Creation(sec(n), o(orderId, n))

  test("if only orders were observed it returns a map with the respective observation states") {
    fakeNewObservation(sec(1), o("A", 10))
    fakeNewObservation(sec(2), o("B", 20))
    fakeNewDisappearance(sec(2), "A")
    fakeEmptyTrades()
    fakeEmptyOperations()
    eval().mapValue shouldEqual Map(
      "A" -> basicOrderTrackingState(
        orderObservationChanges = Seq(
          oc(sec(1), "A", 10),
          oc(sec(2), "A"),
        ),
      ),
      "B" -> basicOrderTrackingState(
        orderObservationChanges = Seq(
          oc(sec(2), "B", 20),
        ),
      ),
    )
  }

  test("trades and operations are combined with the order observations in the result") {
    fakeNewObservation(sec(1), o("A", 10))
    fakeNewOperation(creationEvent("A", 1))
    fakeNewOperation(cancelEvent("A", 2))
    fakeNewTrade(t(1, "A"))
    fakeNewTrade(t(2, "A"))
    eval().mapValue shouldEqual Map(
      "A" -> basicOrderTrackingState(
        operationEvents = Seq(
          creationEvent("A", 1),
          cancelEvent("A", 2),
        ),
        orderObservationChanges = Seq(
          oc(sec(1), "A", 10),
        ),
        tradeEvents = Seq(
          te(1, "A"),
          te(2, "A"),
        ),
      ),
    )
  }

  test("order changes for orders not observed in the history are empty") {
    fakeEmptyObservationEvents()
    fakeNewTrade(t(1, "A"))
    fakeNewOperation(creationEvent("B", 2))
    eval().mapValue shouldEqual Map(
      "A" -> basicOrderTrackingState(
        tradeEvents = Seq(
          te(1, "A")
        ),
        orderObservationChanges = Seq(
        ),
      ),
      "B" -> basicOrderTrackingState(
        operationEvents = Seq(
          creationEvent("B", 2),
        ),
        orderObservationChanges = Seq(
        ),
      )
    )
  }

  test("trades without order id are ignored") {
    fakeNewObservation(sec(1), o("A", 10))
    fakeNewTrade(t(1, "A"))
    fakeNewTrade(t(2, "A").copy(orderId = None))
    fakeEmptyOperations()
    eval().mapValue shouldEqual Map(
      "A" -> basicOrderTrackingState(
        tradeEvents = Seq(te(1, "A")),
        orderObservationChanges = Seq(
          oc(sec(1), "A", 10),
        ),
      )
    )
  }

  test("it is built up incrementally, i.e. states unaffected by updates remain the same") {
    fakeNewObservation(sec(1), o("A", 10))
    fakeNewTrade(t(1, "A"))
    fakeNewOperation(creationEvent("A", 1))
    val stateA = eval().mapValue("A")
    fakeNewTrade(t(2, "B"))
    fakeNewOperation(creationEvent("B", 2))
    fakeNewObservation(sec(1), o("B", 2))
    eval().mapValue("A") shouldBe theSameInstanceAs(stateA)
  }

}
