package io.liquirium.connect

import io.liquirium.core._

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

trait BasicExchangeConnector {

  def loadOpenOrders(tradingPair: TradingPair): Future[Set[Order]]

  def loadCandleHistory(
    tradingPair: TradingPair,
    candleLength: Duration,
    start: Instant,
    maybeEnd: Option[Instant],
  ): Future[CandleHistorySegment]

  def loadTradeHistory(
    tradingPair: TradingPair,
    start: Instant,
    maybeEnd: Option[Instant],
  ): Future[TradeHistorySegment]

  def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]]

}

object BasicExchangeConnector {

  def fromExchangeApi(api: GenericExchangeApi)(implicit ec: ExecutionContext): BasicExchangeConnector =
    new BasicExchangeConnector {

      override def loadOpenOrders(tradingPair: TradingPair): Future[Set[Order]] = api.getOpenOrders(tradingPair)

      override def loadCandleHistory(
        tradingPair: TradingPair,
        candleLength: Duration,
        start: Instant,
        maybeEnd: Option[Instant],
      ): Future[CandleHistorySegment] = {
        val segment = CandleHistorySegment.empty(start, candleLength)
        completeCandleHistorySegment(tradingPair, candleLength, segment, maybeEnd)
      }

      private def completeCandleHistorySegment(
        tradingPair: TradingPair,
        candleLength: Duration,
        chs: CandleHistorySegment,
        maybeEnd: Option[Instant],
      ): Future[CandleHistorySegment] = {
        api.getCandleBatch(tradingPair, candleLength, chs.end).flatMap { b =>
          if (b.start != chs.end)
            throw new RuntimeException(s"Unexpected candle batch start: ${b.start}. Expected ${chs.end}")
          val extendedSegment = chs.extendWith(b.toHistorySegment)
          val finished = (b.nextBatchStart, maybeEnd) match {
            case (None, _) => true
            case (Some(nbs), Some(end)) => !end.isAfter(nbs)
            case _ => false
          }
          if (finished) {
            Future {
              maybeEnd match {
                case Some(end) => extendedSegment.truncate(end).padUntil(end)
                case None => extendedSegment
              }
            }
          }
          else {
            completeCandleHistorySegment(tradingPair, candleLength, extendedSegment, maybeEnd)
          }
        }
      }

      override def loadTradeHistory(
        tradingPair: TradingPair,
        start: Instant,
        maybeEnd: Option[Instant]
      ): Future[TradeHistorySegment] = completeTradeHistorySegment(
          tradingPair,
          start,
          maybeEnd = maybeEnd,
          historySegment = TradeHistorySegment.empty(start),
        )

      private def completeTradeHistorySegment(
        tradingPair: TradingPair,
        start: Instant,
        maybeEnd: Option[Instant],
        historySegment: TradeHistorySegment,
      ): Future[TradeHistorySegment] =
        api.getTradeBatch(tradingPair, start) flatMap { batch =>
          if (batch.start != start)
            throw new RuntimeException(s"Trade batch start ${batch.start} does not equal expected start $start")
          val newSegment: TradeHistorySegment = {
            historySegment.extendWith(maybeEnd match {
              case None => batch.toHistorySegment
              case Some(end) => batch.toHistorySegment.truncate(end)
            })
          }
          if (containsTradeAfterEnd(batch, maybeEnd)) Future(newSegment)
          else batch.nextBatchStart match {
            case Some(start) if maybeEnd.isEmpty || start.isBefore(maybeEnd.get) =>
              completeTradeHistorySegment(tradingPair, start, maybeEnd = maybeEnd, historySegment = newSegment)
            case _ => Future(newSegment)
          }
        }

      private def containsTradeAfterEnd(batch: TradeBatch, maybeEnd: Option[Instant]): Boolean =
        maybeEnd match {
          case None => false
          case Some(end) => batch.trades.lastOption.exists(t => !t.time.isBefore(end))
        }

      override def submitRequest[TR <: OperationRequest](request: TR): Future[OperationRequestSuccessResponse[TR]] =
        api.sendTradeRequest(request)

    }

}
