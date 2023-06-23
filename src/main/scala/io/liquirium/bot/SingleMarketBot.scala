package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, TradeHistoryInput}
import io.liquirium.bot.SingleMarketBot.Strategy
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.{CandleHistorySegment, ExactResources, Market}
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

  trait Strategy extends (SingleMarketBot.State => Seq[OrderIntent]) {

    def candleLength: Duration

    // The SingleMarketBot will always provide a candle history of at least this length
    def minimumCandleHistoryLength: Duration

  }

}

abstract class SingleMarketBot extends EvalBot {

  protected def market: Market

  protected def startTime: Instant

  protected def initialResources: ExactResources

  protected def getOrderIntentConveyor: (Market, Eval[Seq[OrderIntent]]) => Eval[Iterable[BotOutput]]

  protected def strategy: Strategy

  protected def isSimulation: Boolean

  private val candleHistoryInput: CandleHistoryInput =
    CandleHistoryInput(
      market = market,
      start = startTime minus strategy.minimumCandleHistoryLength,
      candleLength = strategy.candleLength,
    )

  private val tradeHistoryInput: TradeHistoryInput =
    TradeHistoryInput(
      market = market,
      start = if (isSimulation) Instant.ofEpochSecond(0) else startTime,
    )

  private val baseBalanceEval = InputEval(tradeHistoryInput).foldIncremental(_ => initialResources.baseBalance) {
    (bb, t) => bb + t.effects.filter(_.ledger == market.baseLedger).map(_.change).sum
  }

  private val quoteBalanceEval = InputEval(tradeHistoryInput).foldIncremental(_ => initialResources.quoteBalance) {
    (qb, t) => qb + t.effects.filter(_.ledger == market.quoteLedger).map(_.change).sum
  }

  private val stateEval: Eval[SingleMarketBot.State] =
    for {
      baseBalance <- baseBalanceEval
      quoteBalance <- quoteBalanceEval
      candleHistorySegment <- InputEval(candleHistoryInput)
    } yield
      SingleMarketBot.State(
        time = candleHistorySegment.end,
        baseBalance = baseBalance,
        quoteBalance = quoteBalance,
        candleHistory = candleHistorySegment,
      )

  override val eval: Eval[Iterable[BotOutput]] = getOrderIntentConveyor(market, stateEval.map(s => strategy(s)))

}
