package io.liquirium.bot.simulation.helpers

import io.liquirium.bot.simulation.SimulationMarketplace
import io.liquirium.bot.{BotInput, OperationRequestMessage}
import io.liquirium.core.Market
import io.liquirium.eval.{InputRequest, InputUpdate, UpdatableContext}
import org.scalatest.Matchers


case class FakeSimulationMarketplace(
  market: Market,
  traderInputs: Map[BotInput[_], Any] = Map(),
  priceUpdateTransitions: Seq[(UpdatableContext, Either[InputRequest, UpdatableContext])] = Seq(),
  operationRequestTransitions: Seq[((OperationRequestMessage, UpdatableContext), Either[InputRequest, InputUpdate])] = Seq(),
  calls: Seq[Any] = Seq(),
) extends SimulationMarketplace with Matchers {

  override def processOperationRequest(
    tr: OperationRequestMessage,
    context: UpdatableContext,
  ): Either[InputRequest, (InputUpdate, SimulationMarketplace)] =
    operationRequestTransitions.headOption match {
      case Some(((r, ctx), Right(iu))) if r == tr && context == ctx =>
        val newState = copy(
          calls = calls :+ tr,
          operationRequestTransitions = operationRequestTransitions.drop(1)
        )
        Right((iu, newState))
      case Some(((r, ctx), Left(ir))) if r == tr && context == ctx => Left(ir)
      case Some((r, _)) => fail(s"marketplaces expected trade request $r")
      case None => fail(s"marketplace expects no more updates")
    }

  override def processPriceUpdates(
    context: UpdatableContext,
  ): Either[InputRequest, (UpdatableContext, SimulationMarketplace)] =
    priceUpdateTransitions.headOption match {
      case Some((ctx, Right(newContext))) if ctx == context => Right((newContext, copy(
        calls = calls :+ context,
        priceUpdateTransitions = priceUpdateTransitions.drop(1)
      )))
      case Some((ctx, Left(inputRequest))) if ctx == context => Left(inputRequest)
      case Some(_) => fail(s"marketplace expected another context")
      case None => fail(s"marketplace expects no more price updates")
    }

  def addPriceUpdateTransition(context: UpdatableContext, newContext: UpdatableContext): FakeSimulationMarketplace =
    copy(priceUpdateTransitions = priceUpdateTransitions :+ (context, Right(newContext)))

  def addPriceUpdateFailure(context: UpdatableContext, inputRequest: InputRequest): FakeSimulationMarketplace =
    copy(priceUpdateTransitions = priceUpdateTransitions :+ (context, Left(inputRequest)))

  def addTradeRequestCompletion(
    tr: OperationRequestMessage,
    context: UpdatableContext,
    inputUpdate: InputUpdate,
  ): FakeSimulationMarketplace = copy(
    operationRequestTransitions = operationRequestTransitions :+ ((tr, context), Right(inputUpdate)),
  )

  def addTradeRequestFailure(
    tr: OperationRequestMessage,
    context: UpdatableContext,
    inputRequest: InputRequest,
  ): FakeSimulationMarketplace = copy(
    operationRequestTransitions = operationRequestTransitions :+ ((tr, context), Left(inputRequest)),
  )

}
