package io.liquirium.connect

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.core._

import java.time.{Duration, Instant}
import scala.concurrent.Future


trait ExchangeConnector {

  def loadCandleHistory(
    tradingPair: TradingPair,
    candleLength: Duration,
    start: Instant,
  ): Future[CandleHistorySegment]

  def loadTradeHistory(tradingPair: TradingPair, start: Instant): Future[TradeHistorySegment]

  def candleHistoryStream(
    tradingPair: TradingPair,
    initialSegment: CandleHistorySegment,
  ): Source[CandleHistorySegment, NotUsed]

  def tradeHistoryStream(
    tradingPair: TradingPair,
    initialSegment: TradeHistorySegment,
  ): Source[TradeHistorySegment, NotUsed]

  def openOrdersStream(tradingPair: TradingPair): Source[Set[Order], NotUsed]

  def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]]

}
