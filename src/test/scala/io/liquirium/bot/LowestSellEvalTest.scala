package io.liquirium.bot

import io.liquirium.bot.BotInput.OrderSnapshotHistoryInput
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.MarketHelpers
import io.liquirium.core.helpers.OrderHelpers.order
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers
import io.liquirium.core.orderTracking.helpers.OrderTrackingHelpers.{openOrdersHistory, openOrdersSnapshot}
import io.liquirium.eval.Constant
import io.liquirium.eval.helpers.EvalTest

class LowestSellEvalTest extends EvalTest {

  private val market = MarketHelpers.m(123)
  private var fallbackEval = Constant(BigDecimal(1))

  private def evaluate(): BigDecimal = {
    evaluate(LowestSellEval(market, fallbackEval)).get
  }

  test("it evaluates to the price of the lowest sell order of the latest snapshot") {
    fakeInput(OrderSnapshotHistoryInput(market), openOrdersHistory(
      openOrdersSnapshot(
        sec(9),
        order("A", quantity = dec(-4), price = dec(10)),
        order("B", quantity = dec(-5), price = dec(8)),
      ),
      openOrdersSnapshot(
        sec(10),
        order("A", quantity = dec(-4), price = dec(10)),
        order("C", quantity = dec(-5), price = dec(7)),
        order("D", quantity = dec(5), price = dec(1)),
      ),
    ))
    evaluate() shouldEqual dec(7)
  }

  test("it evaluates to the fallback if there are no sell orders in the last snapshot") {
    fallbackEval = Constant(dec(123))
    fakeInput(OrderSnapshotHistoryInput(market), openOrdersHistory(
      openOrdersSnapshot(
        sec(9),
        order("A", quantity = dec(-4), price = dec(10)),
      ),
      openOrdersSnapshot(
        sec(10),
        order("D", quantity = dec(5), price = dec(1)),
      ),
    ))
    evaluate() shouldEqual dec(123)
  }

}
