package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, TradeHistoryInput}
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.{CandleHistorySegment, Market}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Eval, InputEval}

import java.time.{Duration, Instant}


object SingleMarketBot {

  case class State(
    time: Instant,
    baseBalance: BigDecimal,
    quoteBalance: BigDecimal,
    candleHistory: CandleHistorySegment,
  )

}

abstract class SingleMarketBot extends EvalBot {

  protected def market: Market

  protected def startTime: Instant

  protected def initialBaseBalance: BigDecimal

  protected def initialQuoteBalance: BigDecimal

  protected def candleLength: Duration

  protected def minimumCandleHistoryLength: Duration

  protected def getOrderIntentConveyor: (Market, Eval[Seq[OrderIntent]]) => Eval[Iterable[BotOutput]]

  private val candleHistoryInput: CandleHistoryInput =
    CandleHistoryInput(
      market = market,
      start = startTime minus minimumCandleHistoryLength,
      candleLength = candleLength,
    )

  private val tradeHistoryInput: TradeHistoryInput =
    TradeHistoryInput(
      market = market,
      start = startTime,
    )

  private val baseBalanceEval = InputEval(tradeHistoryInput).foldIncremental(_ => initialBaseBalance) {
    (bb, t) => bb + t.effects.filter(_.ledger == market.baseLedger).map(_.change).sum
  }

  private val quoteBalanceEval = InputEval(tradeHistoryInput).foldIncremental(_ => initialQuoteBalance) {
    (qb, t) => qb + t.effects.filter(_.ledger == market.quoteLedger).map(_.change).sum
  }

  private val stateEval: Eval[SingleMarketBot.State] =
    for {
      baseBalance <- baseBalanceEval
      quoteBalance <- quoteBalanceEval
      candleHistorySegment <- InputEval(candleHistoryInput)
    } yield {
      SingleMarketBot.State(
        time = candleHistorySegment.end,
        baseBalance = baseBalance,
        quoteBalance = quoteBalance,
        candleHistory = candleHistorySegment,
      )
    }

  override def eval: Eval[Iterable[BotOutput]] =
    getOrderIntentConveyor(market, stateEval.map(s => getOrderIntents(s)))

  protected def getOrderIntents(state: SingleMarketBot.State): Seq[OrderIntent]

}
