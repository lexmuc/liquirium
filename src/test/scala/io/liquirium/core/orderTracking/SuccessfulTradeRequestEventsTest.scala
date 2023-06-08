package io.liquirium.core.orderTracking

import io.liquirium.bot.BotInput.{CompletedOperationRequest, CompletedOperationRequestsInSession}
import io.liquirium.bot.OperationRequestMessage
import io.liquirium.bot.helpers.OperationRequestHelpers._
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.helpers.OrderHelpers
import io.liquirium.core.helpers.TradeHelpers.trade
import io.liquirium.core.{Market, OperationRequest}
import io.liquirium.eval.helpers.EvalTest
import io.liquirium.eval.{IncrementalSeq, InputEval}
import io.liquirium.util.AbsoluteQuantity

class SuccessfulTradeRequestEventsTest extends EvalTest {

  private val market = m(123)
  private val otherMarket = m(999)

  private def o(mkt: Market, n: Int) = OrderHelpers.order(n).copy(id = n.toString, market = mkt)
  private def o(mkt: Market, s: String) = OrderHelpers.order(1).copy(id = s, market = mkt)

  def fakeRequests(completedRequests: CompletedOperationRequest*): Unit = {
    fakeEvalValue(InputEval(CompletedOperationRequestsInSession), IncrementalSeq(completedRequests: _*))
  }

  private def expectElements(ee: OrderTrackingEvent*): Unit = {
    eval() shouldEqual IncrementalSeq(ee: _*)
  }

  private def eval(): IncrementalSeq[OrderTrackingEvent.OperationEvent] =
    evaluate(SuccessfulTradeRequestEvents(market)).get

  private def msg(n: Int, req: OperationRequest): OperationRequestMessage = OperationRequestMessage(id(n), req)

  test("a created order is converted to the respective event") {
    fakeRequests(
      successfulRequestWithTime(sec(10), msg(1, orderRequest(1)), orderConfirmation(o(market, 10)))
    )
    expectElements(OrderTrackingEvent.Creation(sec(10), o(market, 10)))
  }

  test("order is the same as in the completed trade requests") {
    fakeRequests(
      successfulRequestWithTime(sec(9), msg(1, orderRequest(1)), orderConfirmation(o(market, 11))),
      successfulRequestWithTime(sec(10), msg(2, orderRequest(2)), orderConfirmation(o(market, 22))),
    )
    expectElements(
      OrderTrackingEvent.Creation(sec(9), o(market, 11)),
      OrderTrackingEvent.Creation(sec(10), o(market, 22)),
    )
  }

  test("cancel confirmations with and without quantity are converted to cancel events") {
    cancelRequestMessage(1).request
    fakeRequests(
      successfulRequestWithTime(sec(9), msg(1, cancelRequest(o(market, "B"))), cancelConfirmation),
      successfulRequestWithTime(sec(10), msg(2, cancelRequest(o(market, "A"))), cancelConfirmation(dec(4))),
    )
    expectElements(
      OrderTrackingEvent.Cancel(sec(9), "B", None),
      OrderTrackingEvent.Cancel(sec(10), "A", Some(AbsoluteQuantity(dec(4)))),
    )
  }

  test("requests for different markets are ignored") {
    fakeRequests(
      successfulRequestWithTime(sec(9), msg(1, orderRequest(1)), orderConfirmation(o(otherMarket, 11))),
      successfulRequestWithTime(sec(10), msg(2, cancelRequest(o(otherMarket, "A"))), cancelConfirmation(dec(4))),
    )
    expectElements()
  }

  test("order confirmations with only trades are ignored") {
    fakeRequests(
      successfulRequestWithTime(sec(1), msg(1, orderRequest(1)), orderConfirmation(Seq(trade(1))))
    )
    expectElements()
  }

}
