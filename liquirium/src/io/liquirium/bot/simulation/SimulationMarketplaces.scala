package io.liquirium.bot.simulation

import io.liquirium.bot.OperationRequestMessage
import io.liquirium.core.Market
import io.liquirium.eval.{InputRequest, UpdatableContext}

trait SimulationMarketplaces {

  def processOperationRequest(
    requestMessage: OperationRequestMessage,
    context: UpdatableContext,
  ): Either[InputRequest, (UpdatableContext, SimulationMarketplaces)]

  def executeActivatedOrders(
    newContext: UpdatableContext,
  ): Either[InputRequest, (UpdatableContext, SimulationMarketplaces)]

}

object SimulationMarketplaces {

  def apply(
    marketplaces: Traversable[SimulationMarketplace],
  ): SimulationMarketplaces = Impl(
    marketplaces.toSeq,
    _ => {
      val msg = "This instance of SimulationMarketplaces does not support creating new marketplaces"
      throw new UnsupportedOperationException(msg)
    },
  )

  def apply(
    marketplaceFactory: Market => SimulationMarketplace,
  ): SimulationMarketplaces = Impl(Seq(), marketplaceFactory)

  case class Impl(
    marketplaces: Seq[SimulationMarketplace],
    marketplaceFactory: Market => SimulationMarketplace,
  ) extends SimulationMarketplaces {

    private val marketplacesByMarket = marketplaces.map(mp => (mp.market, mp)).toMap

    override def processOperationRequest(
      requestMessage: OperationRequestMessage,
      context: UpdatableContext,
    ): Either[InputRequest, (UpdatableContext, SimulationMarketplaces)] =
      assertMarket(requestMessage.request.market).processRequestInternal(requestMessage, context)

    private def assertMarket(m: Market): Impl =
      if (marketplacesByMarket.contains(m)) this else copy(marketplaces = marketplaces :+ marketplaceFactory(m))

    private def processRequestInternal(
      requestMessage: OperationRequestMessage,
      context: UpdatableContext,
    ): Either[InputRequest, (UpdatableContext, SimulationMarketplaces)] = {
      val market = requestMessage.request.market
      marketplacesByMarket(market).processOperationRequest(requestMessage, context) match {
        case Right((inputUpdate, updatedMarketplace)) =>
          val updatedMarketplaces = marketplaces.map(mp => if (mp.market == market) updatedMarketplace else mp)
          val newState = copy(marketplaces = updatedMarketplaces)
          val newContext = context.update(inputUpdate)
          Right(newContext, newState)
        case Left(inputRequest) => Left(inputRequest)
      }
    }

    override def executeActivatedOrders(
      context: UpdatableContext,
    ): Either[InputRequest, (UpdatableContext, SimulationMarketplaces)] = {
      val z: Either[InputRequest, (UpdatableContext, Impl)] = Right((context, copy(marketplaces = Seq())))
      marketplaces.foldLeft(z) {
        case (Right((c, mm)), mp) =>
          mp.processPriceUpdates(c) match {
            case Right((newC, newMp)) => Right((newC, mm.copy(marketplaces = mm.marketplaces :+ newMp)))
            case Left(ir) => Left(ir)
          }
        case (Left(ir), _) => Left(ir)
      }
    }

  }

}
