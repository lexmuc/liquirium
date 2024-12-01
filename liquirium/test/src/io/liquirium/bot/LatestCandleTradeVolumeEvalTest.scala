package io.liquirium.bot

import io.liquirium.core.helpers.CandleHelpers.candleHistorySegment
import io.liquirium.core.helpers.CoreHelpers.{dec, sec, secs}
import io.liquirium.core.helpers.TradeHelpers.{trade, tradeHistorySegment}
import io.liquirium.core.{CandleHistorySegment, TradeHistorySegment}
import io.liquirium.eval.helpers.EvalTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class LatestCandleTradeVolumeEvalTest extends EvalTest {

  private val candlesEval = testEval[CandleHistorySegment]()
  private val tradesEval = testEval[TradeHistorySegment]()

  private def evaluate(): BigDecimal = {
    evaluate(LatestCandleTradeVolumeEval(candlesEval, tradesEval)).get
  }

  test("it is zero when there are no candles") {
    fakeEvalValue(candlesEval, candleHistorySegment(sec(100), secs(5))())
    fakeEvalValue(tradesEval, tradeHistorySegment(sec(90))(
      trade(id = "A", time = sec(99), quantity = dec(1), price = dec(1)),
      trade(id = "B", time = sec(101), quantity = dec(1), price = dec(1)),
    ))
    evaluate() shouldEqual dec(0)
  }

  test("when there are candles it evaluates to the total volume of the trades in the latest candle") {
    fakeEvalValue(candlesEval, candleHistorySegment(sec(100), secs(5))(1, 2, 3))
    fakeEvalValue(tradesEval, tradeHistorySegment(sec(90))(
      trade(id = "A", time = sec(109), quantity = dec(1), price = dec(1)),
      trade(id = "B", time = sec(110), quantity = dec(7), price = dec(2)),
      trade(id = "C", time = sec(114), quantity = dec(8), price = dec(2)),
      trade(id = "D", time = sec(115), quantity = dec(1), price = dec(1)),
    ))
    evaluate() shouldEqual dec(30)
  }

}
