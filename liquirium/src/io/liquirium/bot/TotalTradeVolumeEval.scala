package io.liquirium.bot

import io.liquirium.core.TradeHistorySegment
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{CaseEval, Eval}

case class TotalTradeVolumeEval(
  tradeHistoryEval: Eval[TradeHistorySegment],
) extends CaseEval[BigDecimal] {

  override protected val baseEval: Eval[BigDecimal] =
    tradeHistoryEval.foldIncremental(_ => BigDecimal(0)) {
      case (acc, trade) => acc + trade.volume
    }

}
