package io.liquirium.bot.simulation

import io.liquirium.bot.OperationRequestMessage
import io.liquirium.core.Market
import io.liquirium.eval.{InputRequest, InputUpdate, UpdatableContext}


trait SimulationMarketplace {

  def market: Market

  def processOperationRequest(
    requestMessage: OperationRequestMessage,
    context: UpdatableContext,
  ): Either[InputRequest, (InputUpdate, SimulationMarketplace)]

  def processPriceUpdates(
    newContext: UpdatableContext,
  ): Either[InputRequest, (UpdatableContext, SimulationMarketplace)]

}
