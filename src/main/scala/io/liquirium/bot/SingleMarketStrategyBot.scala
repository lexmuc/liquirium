package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, TradeHistoryInput}
import io.liquirium.bot.SingleMarketStrategyBot.Strategy
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.{CandleHistorySegment, ExactResources, Market}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Eval, InputEval}

import java.time.{Duration, Instant}


object SingleMarketStrategyBot {

  case class State(
    time: Instant,
    baseBalance: BigDecimal,
    quoteBalance: BigDecimal,
    candleHistory: CandleHistorySegment,
  )

  trait Strategy extends (SingleMarketStrategyBot.State => Seq[OrderIntent]) {

    def candleLength: Duration

    // The SingleMarketBot will always provide a candle history of at least this length
    def minimumCandleHistoryLength: Duration

  }

}

case class SingleMarketStrategyBot(
  market: Market,
  startTime: Instant,
  initialResources: ExactResources,
  orderIntentConveyorEval: Eval[Seq[OrderIntent] => Iterable[BotOutput]],
  strategy: Strategy,
) extends EvalBot {


  private val candleHistoryInput: CandleHistoryInput =
    CandleHistoryInput(
      market = market,
      start = startTime minus strategy.minimumCandleHistoryLength,
      candleLength = strategy.candleLength,
    )

  private val tradeHistoryInput: TradeHistoryInput = TradeHistoryInput(market, startTime)

  private val baseBalanceEval = InputEval(tradeHistoryInput).foldIncremental(_ => initialResources.baseBalance) {
    (bb, t) => bb + t.effects.filter(_.ledger == market.baseLedger).map(_.change).sum
  }

  private val quoteBalanceEval = InputEval(tradeHistoryInput).foldIncremental(_ => initialResources.quoteBalance) {
    (qb, t) => qb + t.effects.filter(_.ledger == market.quoteLedger).map(_.change).sum
  }

  private val stateEval: Eval[SingleMarketStrategyBot.State] =
    for {
      baseBalance <- baseBalanceEval
      quoteBalance <- quoteBalanceEval
      candleHistorySegment <- InputEval(candleHistoryInput)
    } yield
      SingleMarketStrategyBot.State(
        time = candleHistorySegment.end,
        baseBalance = baseBalance,
        quoteBalance = quoteBalance,
        candleHistory = candleHistorySegment,
      )

  override def eval: Eval[Iterable[BotOutput]] =
    for {
      conveyor <- orderIntentConveyorEval
      state <- stateEval
    } yield {
      conveyor(strategy(state))
    }

}
