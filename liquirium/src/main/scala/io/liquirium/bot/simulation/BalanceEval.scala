package io.liquirium.bot.simulation
import io.liquirium.bot.BotInput.TradeHistoryInput
import io.liquirium.core.{LedgerRef, Market}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Eval, InputEval}

import java.time.Instant


object BalanceEval {

  def apply(
    ledgerRef: LedgerRef,
    startTime: Instant,
    initialBalance: BigDecimal,
    tradeMarkets: Iterable[Market],
  ): Eval[BigDecimal] = {
    val tradeHistoryEvals = tradeMarkets.map { market =>
      InputEval(TradeHistoryInput(market, startTime))
    }
    val marketEffects = tradeHistoryEvals.map { the =>
      the.foldIncremental(_ => BigDecimal(0)) {
        (bb, t) => bb + t.effects.filter(_.ledger == ledgerRef).map(_.change).sum
      }
    }
    Eval.sequence(marketEffects).map(_.sum).map(initialBalance + _)
  }

}
