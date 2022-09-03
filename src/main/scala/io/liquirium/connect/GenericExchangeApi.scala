package io.liquirium.connect

import io.liquirium.core._

import java.time.{Duration, Instant}
import scala.concurrent.Future

trait GenericExchangeApi {

  def getCandleBatch(tradingPair: TradingPair, candleLength: Duration, start: Instant): Future[CandleBatch]

  def getTradeBatch(tradingPair: TradingPair, start: Instant): Future[TradeBatch]

  def getOpenOrders(tradingPair: TradingPair): Future[Set[Order]]

  def sendTradeRequest[T <: OperationRequest](tradeRequest: T): Future[OperationRequestSuccessResponse[T]]

  //noinspection AccessorLikeMethodIsEmptyParen
  def getOrderConstraintsByMarket(): Future[Map[Market, OrderConstraints]]

}
