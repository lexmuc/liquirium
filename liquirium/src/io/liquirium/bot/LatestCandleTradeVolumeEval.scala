package io.liquirium.bot

import io.liquirium.core.{CandleHistorySegment, TradeHistorySegment}
import io.liquirium.eval.{CaseEval, Eval}

case class LatestCandleTradeVolumeEval (
  candlesEval: Eval[CandleHistorySegment],
  tradeHistoryEval: Eval[TradeHistorySegment],
) extends CaseEval[BigDecimal] {

  override protected val baseEval: Eval[BigDecimal] =
    Eval.map2(candlesEval, tradeHistoryEval) {
      (candles, tradeHistory) =>
        candles.lastOption match {
          case None => BigDecimal(0)
          case Some(c) =>
            tradeHistory.reverseIterator
              .dropWhile(t => !(t.time isBefore c.endTime))
              .takeWhile(t => !(t.time isBefore c.startTime))
              .map(_.volume)
              .sum
        }
    }

}
