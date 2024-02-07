package io.liquirium.examples.simulation

import io.liquirium.bot.BotInput.{BotOutputHistory, CompletedOperationRequestsInSession, OrderSnapshotHistoryInput, SimulatedOpenOrdersInput, TradeHistoryInput}
import io.liquirium.bot.{BotOutput, SimpleBotEvaluator}
import io.liquirium.bot.simulation.{ContextWithInputResolution, EvalBotSimulator, SimulationEnvironmentProvider, SimulationLogger}
import io.liquirium.core.{Order, OrderSet, TradeHistorySegment}
import io.liquirium.core.orderTracking.{OpenOrdersHistory, OpenOrdersSnapshot}
import io.liquirium.eval.{Eval, IncrementalContext, IncrementalSeq, UpdatableContext}

import java.time.Instant
import scala.concurrent.ExecutionContext

class EvalBotSimulatorFactory(
  simulationEnvironmentProvider: SimulationEnvironmentProvider,
  withTradeHistoryInput: Boolean,
)(
  implicit executionContext: ExecutionContext
) {

  def getSimulator[L <: SimulationLogger[L]](
    simulationStart: Instant,
    simulationEnd: Instant,
    botEval: Eval[Iterable[BotOutput]],
    logger: L,
  ): EvalBotSimulator[L] = {
    val simulationEnvironment =
      simulationEnvironmentProvider.apply(
        simulationStart = simulationStart,
        simulationEnd = simulationEnd,
      )

    EvalBotSimulator[L](
      context = initialContext(simulationStart),
      evaluator = SimpleBotEvaluator(botEval),
      environment = simulationEnvironment,
      logger = logger,
    )
  }

  private def initialContext(simulationStart: Instant): UpdatableContext =
    ContextWithInputResolution(
      baseContext = IncrementalContext(),
      resolve = {
        case BotOutputHistory => Some(IncrementalSeq.empty)
        case CompletedOperationRequestsInSession => Some(IncrementalSeq.empty)
        case OrderSnapshotHistoryInput(_) => Some(
          OpenOrdersHistory.start(OpenOrdersSnapshot(OrderSet.empty, Instant.ofEpochSecond(0)))
        )
        case TradeHistoryInput(_, start) if start == simulationStart && withTradeHistoryInput =>
          Some(TradeHistorySegment.empty(simulationStart))
        case CompletedOperationRequestsInSession => Some(IncrementalSeq.empty)
        case SimulatedOpenOrdersInput(_) => Some(Set[Order]())
        case _ => None
      }
    )

}
