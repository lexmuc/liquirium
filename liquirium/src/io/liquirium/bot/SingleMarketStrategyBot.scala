package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, TradeHistoryInput}
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.core.{CandleHistorySegment, TradeHistorySegment}
import io.liquirium.eval.IncrementalFoldHelpers.IncrementalEval
import io.liquirium.eval.{Eval, InputEval}


case class SingleMarketStrategyBot(
  strategy: SingleMarketStrategy,
  runConfiguration: SingleMarketStrategyBotRunConfiguration,
  orderIntentConveyorEval: Eval[Seq[OrderIntent] => Iterable[BotOutput]],
) extends EvalBot {

  private val market = runConfiguration.market
  private val startTime = runConfiguration.operationPeriod.start
  private val initialResources = runConfiguration.initialResources

  val candleHistoryEval: Eval[CandleHistorySegment] =
    InputEval(
      CandleHistoryInput(
        market = market,
        start = startTime minus strategy.minimumCandleHistoryLength,
        candleLength = strategy.candleLength,
      )
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
      candleHistorySegment <- candleHistoryEval
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
        if ((state.time isBefore startTime) || !(state.time isBefore runConfiguration.operationPeriod.end))
          Seq()
        else strategy(state)
      conveyor(intents)
    }

}
