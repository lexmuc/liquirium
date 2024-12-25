package io.liquirium.connect
import io.liquirium.core._
import io.liquirium.util.async.{Scheduler, Subscription}

import java.time.{Duration, Instant}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class PollingExchangeConnector(
  basicConnector: BasicExchangeConnector,
  scheduler: Scheduler,
  openOrderPollingInterval: FiniteDuration,
  candlePollingInterval: FiniteDuration,
  candleUpdateOverlapStrategy: CandleUpdateOverlapStrategy,
  tradePollingInterval: FiniteDuration,
  tradeUpdateOverlapStrategy: TradeUpdateOverlapStrategy,
) extends ExchangeConnectorWithSubscriptions {

  override def loadOpenOrders(tradingPair: TradingPair): Future[Set[Order]] =
    basicConnector.loadOpenOrders(tradingPair)

  override def loadCandleHistory(
    tradingPair: TradingPair,
    candleLength: Duration,
    start: Instant,
    maybeEnd: Option[Instant]
  ): Future[CandleHistorySegment] =
    basicConnector.loadCandleHistory(tradingPair, candleLength, start, maybeEnd = maybeEnd)

  override def loadTradeHistory(
    tradingPair: TradingPair,
    start: Instant,
    maybeEnd: Option[Instant],
  ): Future[TradeHistorySegment] =
    basicConnector.loadTradeHistory(tradingPair, start, maybeEnd = maybeEnd)

  override def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]] =
    basicConnector.submitRequest(request)

  override def candleHistorySubscription(
    tradingPair: TradingPair,
    initialSegment: CandleHistorySegment,
  ): Subscription[CandleHistorySegment] =
    PollingCandleHistorySubscription(
      initialSegment = initialSegment,
      scheduler = scheduler,
      pollingInterval = candlePollingInterval,
      updateOverlapStrategy = candleUpdateOverlapStrategy,
      loadSegment = start => this.loadCandleHistory(tradingPair, initialSegment.candleLength, start, maybeEnd = None)
    )

  override def tradeHistorySubscription(tradingPair: TradingPair,
    initialSegment: TradeHistorySegment,
  ): Subscription[TradeHistorySegment] =
    PollingTradeHistorySubscription(
      initialSegment = initialSegment,
      scheduler = scheduler,
      pollingInterval = tradePollingInterval,
      loadSegment = start => this.loadTradeHistory(tradingPair, start, maybeEnd = None),
      updateOverlapStrategy = tradeUpdateOverlapStrategy,
    )

  override def openOrdersSubscription(tradingPair: TradingPair): Subscription[Set[Order]] =
    PollingOpenOrdersSubscription(
      scheduler = scheduler,
      pollingInterval = openOrderPollingInterval,
      loadOrders = () => this.loadOpenOrders(tradingPair),
    )

}
