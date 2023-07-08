package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, TradeHistoryInput}
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.TradeHistorySegment
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Eval, InputEval}


case class SingleMarketStrategyBot(
  strategy: SingleMarketStrategy,
  runConfiguration: SingleMarketBotRunConfiguration,
  orderIntentConveyorEval: Eval[Seq[OrderIntent] => Iterable[BotOutput]],
) extends EvalBot {

  private val market = runConfiguration.market
  private val startTime = runConfiguration.startTime
  private val initialResources = runConfiguration.initialResources

  private val candleHistoryInput: CandleHistoryInput =
    CandleHistoryInput(
      market = market,
      start = startTime minus strategy.minimumCandleHistoryLength,
      candleLength = strategy.candleLength,
    )

  val tradeHistoryEval: Eval[TradeHistorySegment] = InputEval(TradeHistoryInput(market, startTime))

  val baseBalanceEval: Eval[BigDecimal] =
    tradeHistoryEval.foldIncremental(_ => initialResources.baseBalance) {
      (bb, t) => bb + t.effects.filter(_.ledger == market.baseLedger).map(_.change).sum
    }

  val quoteBalanceEval: Eval[BigDecimal] =
    tradeHistoryEval.foldIncremental(_ => initialResources.quoteBalance) {
      (qb, t) => qb + t.effects.filter(_.ledger == market.quoteLedger).map(_.change).sum
    }

  private val stateEval: Eval[SingleMarketStrategy.State] =
    for {
      baseBalance <- baseBalanceEval
      quoteBalance <- quoteBalanceEval
      candleHistorySegment <- InputEval(candleHistoryInput)
    } yield
      SingleMarketStrategy.State(
        time = candleHistorySegment.end,
        baseBalance = baseBalance,
        quoteBalance = quoteBalance,
        candleHistory = candleHistorySegment,
        runConfiguration = runConfiguration,
      )

  override def eval: Eval[Iterable[BotOutput]] =
    for {
      conveyor <- orderIntentConveyorEval
      state <- stateEval
    } yield {
      val intents =
        if (state.time isBefore startTime) Seq()
        else if (state.runConfiguration.endTimeOption.isDefined
          && !(state.time isBefore state.runConfiguration.endTimeOption.get)) Seq()
        else strategy(state)
      conveyor(intents)
    }

}
