package io.liquirium.bot

import io.liquirium.core.TradeHistorySegment
import io.liquirium.core.helpers.CoreHelpers.{dec, sec}
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import io.liquirium.eval.helpers.EvalTest

class TotalTradeVolumeEvalTest extends EvalTest {

  private val tradesEval = testEval[TradeHistorySegment]()

  private def evaluate(): BigDecimal = {
    evaluate(TotalTradeVolumeEval(tradesEval)).get
  }

  test("it evaluates to the total trade volume of the given trade history segment") {
    fakeEvalValue(tradesEval, tradeHistorySegment(sec(90))(
      trade(id = "A", time = sec(99), quantity = dec(2), price = dec(10)),
      trade(id = "B", time = sec(101), quantity = dec(-4), price = dec(7)),
    ))
    evaluate() shouldEqual dec(20) + dec(28)
  }

}
