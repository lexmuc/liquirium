package io.liquirium.bot.simulation

import io.liquirium.bot.{BotEvaluator, BotOutput}
import io.liquirium.eval.{InputRequest, UpdatableContext, Value}

import scala.annotation.tailrec


case class EvalBotSimulator[L <: SimulationLogger[L]](
  context: UpdatableContext,
  evaluator: BotEvaluator,
  environment: SimulationEnvironment,
  logger: L,
) {

  def simulateOneStep(): EvalBotSimulator[L] = {
    val (outputs, environmentAfterBot, contextAfterBot) = evaluateBot(context, environment)
    val (contextAfterOutputProcessing, newEnvironment) = environmentAfterBot.processOutputs(outputs, contextAfterBot)
    if (outputs.isEmpty) {
      val (newLogger, contextAfterLogging, environmentAfterLogging) =
        log(logger, contextAfterOutputProcessing, newEnvironment)
      val (finalContext, finalEnvironment) = environmentAfterLogging.advance(contextAfterLogging)
      copy(
        context = finalContext,
        environment = finalEnvironment,
        logger = newLogger,
      )
    }
    else copy(
      context = contextAfterOutputProcessing,
      environment = newEnvironment,
    )
  }

  def run(): L = {
    @tailrec
    def go(simulator: EvalBotSimulator[L]): L =
      if (simulator.environment.isSimulationComplete) simulator.logger
      else go(simulator.simulateOneStep())
    go(this)
  }

  @tailrec
  private def evaluateBot(
    c: UpdatableContext,
    e: SimulationEnvironment,
  ): (Seq[BotOutput], SimulationEnvironment, UpdatableContext) =
    evaluator.eval(c) match {
      case (Value(oo), newContext) => (oo, e, newContext)
      case (ir: InputRequest, newContext) =>
        val (updatedContext, newEnvironment) = e.getInputs(ir, newContext)
        evaluateBot(updatedContext, newEnvironment)
    }

  @tailrec
  private def log(
    logger: L,
    context: UpdatableContext,
    environment: SimulationEnvironment,
  ): (L, UpdatableContext, SimulationEnvironment) =
    logger.log(context) match {
      case (Value(newLogger), newContext) => (newLogger, newContext, environment)
      case (ir: InputRequest, newContext) =>
        val (updatedContext, newEnvironment) = environment.getInputs(ir, newContext)
        log(logger, updatedContext, newEnvironment)
    }

}
