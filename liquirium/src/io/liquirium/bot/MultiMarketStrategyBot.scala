package io.liquirium.bot

import io.liquirium.bot.BotInput.{CandleHistoryInput, TimeInput, TradeHistoryInput}
import io.liquirium.bot.simulation.BalanceEval
import io.liquirium.core.{CandleHistorySegment, LedgerRef, Market, TradeHistorySegment}
import io.liquirium.core.OperationIntent.OrderIntent
import io.liquirium.eval.{Eval, InputEval}

case class MultiMarketStrategyBot(
  strategy: MultiMarketStrategy,
  runConfiguration: MultiMarketStrategyBotRunConfiguration,
  orderIntentConveyorEval: Eval[Seq[OrderIntent] => Iterable[BotOutput]],
) extends EvalBot {

  val candleHistoryEvalsByMarket: Map[Market, Eval[CandleHistorySegment]] =
    runConfiguration.markets.toSeq.map(market =>
      market -> InputEval(
        CandleHistoryInput(
          market = market,
          start = runConfiguration.operationPeriod.start minus strategy.minimumCandleHistoryLength,
          candleLength = strategy.candleLength,
        )
      )
    ).toMap

  val tradeHistoryEvalsByMarket: Map[Market, Eval[TradeHistorySegment]] =
    runConfiguration.markets.toSeq.map(market =>
      market -> InputEval(
        TradeHistoryInput(
          market = market,
          start = runConfiguration.operationPeriod.start,
        )
      )
    ).toMap

  private val ledgers: Seq[LedgerRef] = runConfiguration.initialBalances.keySet.toSeq

  private val candleHistoriesByMarketEval =
    Eval.sequence(
      candleHistoryEvalsByMarket.map { case (market, eval) =>
        eval.map(candleHistory => market -> candleHistory)
      }
    ).map(_.toMap)

  val balanceEvalsByLedgerRef: Map[LedgerRef, Eval[BigDecimal]] =
    ledgers.map { l =>
      val be = BalanceEval(
        ledgerRef = l,
        startTime = runConfiguration.operationPeriod.start,
        initialBalance = runConfiguration.initialBalances(l),
        tradeMarkets = runConfiguration.markets.filter(m => m.quoteLedger == l || m.baseLedger == l),
      )
      (l, be)
    }.toMap

  private val balancesByLedgerRefEval =
    Eval.sequence(
      balanceEvalsByLedgerRef.map { case (lr, be) => be.map(b => (lr, b)) }
    ).map(_.toMap)

  val stateEval: Eval[MultiMarketStrategy.State] =
    for {
      candleHistoriesByMarket <- candleHistoriesByMarketEval
      time <- InputEval(TimeInput(strategy.candleLength))
      balancesByLedgerRef <- balancesByLedgerRefEval
    } yield {
      MultiMarketStrategy.State(
        time = time,
        candleHistoriesByMarket = candleHistoriesByMarket,
        runConfiguration = runConfiguration,
        balances = balancesByLedgerRef,
      )
    }


  override def eval: Eval[Iterable[BotOutput]] =
    for {
      conveyor <- orderIntentConveyorEval
      state <- stateEval
    } yield {
      val intents =
        if (
          (state.time isBefore runConfiguration.operationPeriod.start)
            || !(state.time isBefore runConfiguration.operationPeriod.end)
        ) Seq()
        else strategy(state)
      conveyor(intents)
    }


}
