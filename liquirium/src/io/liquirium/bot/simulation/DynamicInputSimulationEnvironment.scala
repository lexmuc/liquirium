package io.liquirium.bot.simulation

import io.liquirium.bot.{BotOutput, OperationRequestMessage}
import io.liquirium.eval.{InputRequest, UpdatableContext}

import scala.annotation.tailrec

case class DynamicInputSimulationEnvironment(
  inputUpdateStream: SimulationInputUpdateStream,
  marketplaces: SimulationMarketplaces,
) extends SimulationEnvironment {

  override def getInputs(
    inputRequest: InputRequest,
    context: UpdatableContext,
  ): (UpdatableContext, DynamicInputSimulationEnvironment) = {
    val newStream = inputUpdateStream.processInputRequest(inputRequest)
    val streamUpdate = newStream.currentInputUpdate.get
    (context.update(streamUpdate), copy(
      inputUpdateStream = newStream,
    ))
  }

  override def processOutputs(
    outputs: Seq[BotOutput],
    context: UpdatableContext,
  ): (UpdatableContext, DynamicInputSimulationEnvironment) =
    if (outputs.isEmpty) (context, this)
    else outputs.collect { case m: OperationRequestMessage => m }.foldLeft((context, this)) {
      case ((c, env), o) => env.processSingleOutput(o, c)
    }

  @tailrec
  private def processSingleOutput(
    o: OperationRequestMessage,
    context: UpdatableContext,
  ): (UpdatableContext, DynamicInputSimulationEnvironment) =
    marketplaces.processOperationRequest(o, context) match {
      case Right((nextContext, nextMarketplaces)) => (nextContext, copy(marketplaces = nextMarketplaces))
      case Left(inputRequest) =>
        val nextStream = inputUpdateStream.processInputRequest(inputRequest)
        val nextContext = context.update(nextStream.currentInputUpdate.get)
        copy(inputUpdateStream = nextStream).processSingleOutput(o, nextContext)
    }

  override def advance(context: UpdatableContext): (UpdatableContext, DynamicInputSimulationEnvironment) = {
    val newStream = inputUpdateStream.advance
    newStream.currentInputUpdate match {
      case None => (context, copy(inputUpdateStream = newStream))
      case Some(iu) => copy(inputUpdateStream = newStream).fillOrdersWithRetry(context.update(iu))
    }
  }

  @tailrec
  private def fillOrdersWithRetry(context: UpdatableContext): (UpdatableContext, DynamicInputSimulationEnvironment) =
    marketplaces.executeActivatedOrders(context) match {
      case Right((nextContext, nextMarketplaces)) =>
        (nextContext, copy(marketplaces = nextMarketplaces))
      case Left(inputRequest) =>
        val nextStream = inputUpdateStream.processInputRequest(inputRequest)
        val newContext = context.update(nextStream.currentInputUpdate.get)
        copy(inputUpdateStream = nextStream).fillOrdersWithRetry(newContext)
    }

  override def isSimulationComplete: Boolean = inputUpdateStream.currentInputUpdate.isEmpty

}
