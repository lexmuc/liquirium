package io.liquirium.bot.simulation

import io.liquirium.bot.{BotEvaluator, BotOutput}
import io.liquirium.eval.{InputRequest, UpdatableContext, Value}


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

  def evaluateBot(
    c: UpdatableContext,
    e: SimulationEnvironment,
  ): (Seq[BotOutput], SimulationEnvironment, UpdatableContext) =
    evaluator.eval(c) match {
      case (Value(oo), newContext) => (oo, e, newContext)
      case (ir: InputRequest, newContext) =>
        val (updatedContext, newEnvironment) = e.getInputs(ir, newContext)
        evaluateBot(updatedContext, newEnvironment)
    }

  def log(l: L, c: UpdatableContext, e: SimulationEnvironment): (L, UpdatableContext, SimulationEnvironment) =
    l.log(c) match {
      case (Value(newLogger), newContext) => (newLogger, newContext, e)
      case (ir: InputRequest, newContext) =>
        val (updatedContext, newEnvironment) = e.getInputs(ir, newContext)
        log(l, updatedContext, newEnvironment)
    }

}
