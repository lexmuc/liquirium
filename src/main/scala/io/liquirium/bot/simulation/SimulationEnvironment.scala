package io.liquirium.bot.simulation

import io.liquirium.bot.BotOutput
import io.liquirium.eval.{InputRequest, UpdatableContext}

trait SimulationEnvironment {

  def getInputs(inputRequest: InputRequest, context: UpdatableContext): (UpdatableContext, SimulationEnvironment)

  def processOutputs(outputs: Seq[BotOutput], context: UpdatableContext): (UpdatableContext, SimulationEnvironment)

  def advance(context: UpdatableContext): (UpdatableContext, SimulationEnvironment)

  def isSimulationComplete: Boolean

}
